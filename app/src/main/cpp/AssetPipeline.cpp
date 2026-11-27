#include "aster/AssetPipeline.hpp"
#include "cgltf.h"
#define STB_IMAGE_IMPLEMENTATION
#define STBI_NO_STDIO
#include "stb_image.h"
#include <algorithm>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <memory>

namespace aster {
namespace {
struct DataDeleter { void operator()(cgltf_data* p) const { if (p) cgltf_free(p); } };
std::string ParentPath(const std::string& p) {
    const auto pos=p.find_last_of("/\\");
    return pos==std::string::npos?std::string{}:p.substr(0,pos+1);
}
bool ReadFile(const std::string& path,std::vector<unsigned char>& out) {
    std::ifstream f(path,std::ios::binary);
    if(!f)return false;
    f.seekg(0,std::ios::end);const auto size=f.tellg();
    if(size<0)return false;
    out.resize(static_cast<std::size_t>(size));f.seekg(0,std::ios::beg);
    return out.empty()||static_cast<bool>(f.read(reinterpret_cast<char*>(out.data()),size));
}
std::vector<unsigned char> DecodeDataUri(const char* uri) {
    std::vector<unsigned char> out;
    if(!uri||std::strncmp(uri,"data:",5)!=0)return out;
    const char* comma=std::strchr(uri,',');if(!comma)return out;
    const char* p=comma+1;
    if(comma-uri<7||std::string(comma-7,comma) != ";base64")return out;
    unsigned int accumulator=0;int bits=0;
    for(;*p&&*p!='=';++p){
        const char* alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        const char* found=std::strchr(alphabet,*p);if(!found)continue;
        accumulator=(accumulator<<6)|static_cast<unsigned int>(found-alphabet);bits+=6;
        if(bits>=8){bits-=8;out.push_back(static_cast<unsigned char>((accumulator>>bits)&255u));}
    }
    return out;
}
bool DecodeImage(const cgltf_image& image,const std::string& base,ImportedTexture& out) {
    std::vector<unsigned char> encoded;
    if(image.uri){
        if(std::strncmp(image.uri,"data:",5)==0)encoded=DecodeDataUri(image.uri);
        else {out.sourcePath=base+image.uri;if(!ReadFile(out.sourcePath,encoded))return false;}
    } else if(image.buffer_view&&image.buffer_view->buffer&&image.buffer_view->buffer->data){
        const auto* begin=static_cast<const unsigned char*>(image.buffer_view->buffer->data)+image.buffer_view->offset;
        encoded.assign(begin,begin+image.buffer_view->size);
    } else return false;
    if(encoded.empty())return false;
    int w=0,h=0,c=0;stbi_uc* pixels=stbi_load_from_memory(encoded.data(),static_cast<int>(encoded.size()),&w,&h,&c,4);
    if(!pixels||w<=0||h<=0){if(pixels)stbi_image_free(pixels);return false;}
    out.width=w;out.height=h;out.channels=4;
    out.rgba.assign(pixels,pixels+static_cast<std::size_t>(w)*static_cast<std::size_t>(h)*4u);
    stbi_image_free(pixels);return true;
}
} // namespace

ImportResult GltfImporter::Load(const std::string& path) {
    ImportResult result;result.model.sourcePath=path;
    const auto ext=std::filesystem::path(path).extension().string();
    if(ext!=".gltf"&&ext!=".glb"){result.error="Only .gltf and .glb files are supported";return result;}
    cgltf_options options{};cgltf_data* raw=nullptr;
    if(cgltf_parse_file(&options,path.c_str(),&raw)!=cgltf_result_success){result.error="Failed to parse glTF/GLB file";return result;}
    std::unique_ptr<cgltf_data,DataDeleter> data(raw);
    if(cgltf_load_buffers(&options,data.get(),path.c_str())!=cgltf_result_success){result.error="Failed to load glTF buffers";return result;}
    const std::string base=ParentPath(path);
    result.model.materials.reserve(data->materials_count);
    for(cgltf_size i=0;i<data->materials_count;++i){
        const auto& src=data->materials[i];ImportedMaterial m;
        if(src.name)m.name=src.name;
        const auto& pbr=src.pbr_metallic_roughness;
        std::copy(pbr.base_color_factor,pbr.base_color_factor+4,m.baseColor);
        m.metallic=std::clamp(static_cast<float>(pbr.metallic_factor),0.0f,1.0f);
        m.roughness=std::clamp(static_cast<float>(pbr.roughness_factor),0.04f,1.0f);
        if(pbr.base_color_texture.texture&&pbr.base_color_texture.texture->image){
            const cgltf_image* image=pbr.base_color_texture.texture->image;
            const int index=static_cast<int>(image-data->images);
            if(index>=0&&index<static_cast<int>(data->images_count)){
                if(result.model.textures.size()<=static_cast<std::size_t>(index))result.model.textures.resize(static_cast<std::size_t>(index)+1);
                auto& tex=result.model.textures[static_cast<std::size_t>(index)];
                if(tex.rgba.empty()&&!DecodeImage(*image,base,tex)){result.error="Failed to decode base-color texture";return result;}
                m.baseColorTexture=index;
            }
        }
        result.model.materials.push_back(std::move(m));
    }
    for(cgltf_size mi=0;mi<data->meshes_count;++mi){
        const auto& src=data->meshes[mi];ImportedMesh mesh;if(src.name)mesh.name=src.name;
        for(cgltf_size pi=0;pi<src.primitives_count;++pi){
            const auto& primitive=src.primitives[pi];
            if(primitive.type!=cgltf_primitive_type_triangles)continue;
            const cgltf_accessor* positions=nullptr;const cgltf_accessor* normals=nullptr;const cgltf_accessor* uvs=nullptr;
            for(cgltf_size ai=0;ai<primitive.attributes_count;++ai){
                const auto& a=primitive.attributes[ai];
                if(a.type==cgltf_attribute_type_position)positions=a.data;
                else if(a.type==cgltf_attribute_type_normal)normals=a.data;
                else if(a.type==cgltf_attribute_type_texcoord&&a.index==0)uvs=a.data;
            }
            if(!positions||positions->type!=cgltf_type_vec3){result.error="Primitive is missing POSITION vec3";return result;}
            ImportedPrimitive out;out.vertices.resize(positions->count);
            if(primitive.material)out.materialIndex=static_cast<int>(primitive.material-data->materials);
            for(cgltf_size vi=0;vi<positions->count;++vi){
                auto& v=out.vertices[vi];cgltf_accessor_read_float(positions,vi,v.position,3);
                if(normals&&normals->count==positions->count)cgltf_accessor_read_float(normals,vi,v.normal,3);
                if(uvs&&uvs->count==positions->count)cgltf_accessor_read_float(uvs,vi,v.uv,2);
            }
            if(primitive.indices){
                out.indices.resize(primitive.indices->count);
                for(cgltf_size ii=0;ii<primitive.indices->count;++ii)out.indices[ii]=static_cast<std::uint32_t>(cgltf_accessor_read_index(primitive.indices,ii));
            } else {
                out.indices.resize(out.vertices.size());
                for(std::size_t ii=0;ii<out.indices.size();++ii)out.indices[ii]=static_cast<std::uint32_t>(ii);
            }
            if(out.indices.size()%3!=0){result.error="Triangle index count is not divisible by three";return result;}
            mesh.primitives.push_back(std::move(out));
        }
        result.model.meshes.push_back(std::move(mesh));
    }
    if(result.model.meshes.empty()){result.error="No triangle meshes found in file";return result;}
    return result;
}
} // namespace aster
