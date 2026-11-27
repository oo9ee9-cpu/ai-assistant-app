#pragma once
#include "aster/AssetPipeline.hpp"
#include <GLES3/gl3.h>
#include <vector>
namespace aster {
// Owns GPU copies of decoded glTF textures. Create/use/destroy on the GL thread.
class OpenGLES3Assets {
public:
    OpenGLES3Assets()=default;
    ~OpenGLES3Assets();
    OpenGLES3Assets(const OpenGLES3Assets&)=delete;
    OpenGLES3Assets& operator=(const OpenGLES3Assets&)=delete;
    bool Upload(const ImportedModel& model);
    void BindBaseColor(const ImportedMaterial& material,GLuint textureUnit=0) const;
    void Clear();
    const std::vector<GLuint>& Textures() const { return textures_; }
private:
    std::vector<GLuint> textures_;
};
} // namespace aster
