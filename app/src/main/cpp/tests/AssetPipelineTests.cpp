#include "aster/AssetPipeline.hpp"
#include <cassert>
#include <filesystem>
#include <fstream>
#include <string>
int main(){
    const auto path=std::filesystem::temp_directory_path()/"aster_invalid.gltf";
    {std::ofstream f(path);f<<"{ not valid gltf }";}
    const auto invalid=aster::GltfImporter::Load(path.string());
    assert(!invalid);
    assert(!invalid.error.empty());
    std::filesystem::remove(path);
    const auto unsupported=aster::GltfImporter::Load("asset.obj");
    assert(!unsupported&&unsupported.error.find("Only .gltf")!=std::string::npos);
    return 0;
}
