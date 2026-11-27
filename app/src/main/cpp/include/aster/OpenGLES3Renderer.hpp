#pragma once
#include "aster/IRenderer.hpp"
#include "aster/EngineRuntime.hpp"
namespace aster{
class OpenGLES3Renderer final:public IRenderer{
public:
 OpenGLES3Renderer(const Scene*s,Engine*e):scene_(s),engine_(e){}
 void Initialize()override;void Resize(int,int)override;void RenderFrame()override;void Shutdown()override;
 int PickGizmoAxis(float screenX,float screenY)const;
 void DragGizmoAxis(int axis,float deltaX,float deltaY);
private:
 void DrawLines(const Mat4&);
 Mat4 ViewProjection()const;
 const Scene*scene_;Engine*engine_;int width_{1},height_{1};
 unsigned int program_{0},lineProgram_{0},vao_{0},vbo_{0};int mvp_{-1},model_{-1},lineMvp_{-1};
};
}
