#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace aster {
struct ImportedVertex {
    float position[3]{};
    float normal[3]{0.0f, 1.0f, 0.0f};
    float uv[2]{};
};
struct ImportedTexture {
    int width{0}, height{0}, channels{4};
    std::vector<std::uint8_t> rgba;
    std::string sourcePath;
};
struct ImportedMaterial {
    std::string name;
    float baseColor[4]{1,1,1,1};
    float metallic{1.0f};
    float roughness{1.0f};
    int baseColorTexture{-1};
};
struct ImportedPrimitive {
    std::vector<ImportedVertex> vertices;
    std::vector<std::uint32_t> indices;
    int materialIndex{-1};
};
struct ImportedMesh {
    std::string name;
    std::vector<ImportedPrimitive> primitives;
};
struct ImportedModel {
    std::vector<ImportedMesh> meshes;
    std::vector<ImportedMaterial> materials;
    std::vector<ImportedTexture> textures;
    std::string sourcePath;
};
struct ImportResult {
    ImportedModel model;
    std::string error;
    explicit operator bool() const { return error.empty(); }
};
class GltfImporter {
public:
    // Loads .gltf/.glb, external/data/buffer-view images and triangle primitives.
    static ImportResult Load(const std::string& path);
};
} // namespace aster
