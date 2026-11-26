#pragma once
#include "aster/IRenderer.hpp"
#include "aster/EngineRuntime.hpp"
namespace aster{
class OpenGLES3Renderer final:public IRenderer{
public:OpenGLES3Renderer(const Scene*s,const Engine*e):scene_(s),engine_(e){}
 void Initialize()override;void Resize(int,int)override;void RenderFrame()override;void Shutdown()override;
private:void DrawLines(const Mat4&);
 const Scene*scene_;const Engine*engine_;int width_{1},height_{1};
 unsigned int program_{0},lineProgram_{0},vao_{0},vbo_{0};int mvp_{-1},model_{-1},lineMvp_{-1};
};
}
