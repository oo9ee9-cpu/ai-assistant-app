#pragma once
#include "aster/IRenderer.hpp"
#include "aster/EngineRuntime.hpp"
#include "aster/AssetPipeline.hpp"
#include "aster/OpenGLES3Assets.hpp"
#include <memory>
#include <string>
#include <vector>
namespace aster{
class OpenGLES3Renderer final:public IRenderer{
public:
 OpenGLES3Renderer(const Scene*s,Engine*e):scene_(s),engine_(e){}
 void Initialize()override;void Resize(int,int)override;void RenderFrame()override;void Shutdown()override;
 int PickGizmoAxis(float,float)const;int PickEntity(float,float)const;void DragGizmoAxis(int,float,float);
 bool ImportModel(const std::string&,std::string&);
private:
 struct GpuPrimitive{unsigned int vao{0},vbo{0},ebo{0};int indexCount{0},materialIndex{-1};};
 struct GpuModel{std::vector<GpuPrimitive> primitives;OpenGLES3Assets assets;std::vector<ImportedMaterial> materials;};
 void DrawLines(const Mat4&);Mat4 ViewProjection()const;
 const Scene*scene_;Engine*engine_;int width_{1},height_{1};
 unsigned int program_{0},lineProgram_{0},vao_{0},vbo_{0},sphereVao_{0},sphereVbo_{0};
 int sphereVertexCount_{0};
 int mvp_{-1},model_{-1},lineMvp_{-1},baseColor_{-1},baseTexture_{-1},hasTexture_{-1};
 std::vector<std::unique_ptr<GpuModel>> imported_;
};
}