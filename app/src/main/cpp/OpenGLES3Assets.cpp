#include "aster/OpenGLES3Assets.hpp"
#include <algorithm>
namespace aster {
OpenGLES3Assets::~OpenGLES3Assets(){Clear();}
bool OpenGLES3Assets::Upload(const ImportedModel& model){
    std::vector<GLuint> pending(model.textures.size(),0);
    glPixelStorei(GL_UNPACK_ALIGNMENT,1);
    for(std::size_t i=0;i<model.textures.size();++i){
        const auto& image=model.textures[i];
        if(image.rgba.empty())continue;
        if(image.width<=0||image.height<=0||image.rgba.size()!=static_cast<std::size_t>(image.width)*static_cast<std::size_t>(image.height)*4u){for(GLuint made:pending)if(made)glDeleteTextures(1,&made);return false;}
        glGenTextures(1,&pending[i]);glBindTexture(GL_TEXTURE_2D,pending[i]);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,image.width,image.height,0,GL_RGBA,GL_UNSIGNED_BYTE,image.rgba.data());
        glGenerateMipmap(GL_TEXTURE_2D);
    }
    glBindTexture(GL_TEXTURE_2D,0);
    Clear();textures_=std::move(pending);return true;
}
void OpenGLES3Assets::BindBaseColor(const ImportedMaterial& material,GLuint unit)const{
    glActiveTexture(GL_TEXTURE0+unit);
    if(material.baseColorTexture>=0&&static_cast<std::size_t>(material.baseColorTexture)<textures_.size())
        glBindTexture(GL_TEXTURE_2D,textures_[static_cast<std::size_t>(material.baseColorTexture)]);
    else glBindTexture(GL_TEXTURE_2D,0);
}
void OpenGLES3Assets::Clear(){for(GLuint id:textures_)if(id)glDeleteTextures(1,&id);textures_.clear();}
} // namespace aster
