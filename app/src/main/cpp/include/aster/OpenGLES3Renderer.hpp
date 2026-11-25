#pragma once
#include "aster/IRenderer.hpp"
#include "aster/EngineRuntime.hpp"
namespace aster {
class OpenGLES3Renderer final:public IRenderer {
public:
 explicit OpenGLES3Renderer(const Scene* scene):scene_(scene){}
 void Initialize()override; void Resize(int width,int height)override;
 void RenderFrame()override; void Shutdown()override;
private:
 const Scene* scene_; int width_{1},height_{1};
 unsigned int program_{0},vao_{0},vbo_{0}; int mvp_{-1},model_{-1};
};
}
