#include "aster/OpenGLES3Renderer.hpp"
#include <GLES3/gl3.h>
#include <GLES3/gl3.h>
#include <algorithm>
#include <array>
#include <cmath>
#include <limits>
#include <vector>
namespace aster { namespace {
GLuint CompileShader(GLenum type,const char*source){
 GLuint shader=glCreateShader(type);glShaderSource(shader,1,&source,nullptr);glCompileShader(shader);
 GLint ok=GL_FALSE;glGetShaderiv(shader,GL_COMPILE_STATUS,&ok);
 if(ok!=GL_TRUE){glDeleteShader(shader);return 0;}return shader;
}
GLuint LinkProgram(const char*vs,const char*fs){
 GLuint v=CompileShader(GL_VERTEX_SHADER,vs),f=CompileShader(GL_FRAGMENT_SHADER,fs);
 if(!v||!f){if(v)glDeleteShader(v);if(f)glDeleteShader(f);return 0;}
 GLuint p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glLinkProgram(p);
 glDeleteShader(v);glDeleteShader(f);GLint ok=GL_FALSE;glGetProgramiv(p,GL_LINK_STATUS,&ok);
 if(ok!=GL_TRUE){glDeleteProgram(p);return 0;}return p;
}
const char* meshVertex=R"(#version 300 es
layout(location=0) in vec3 aPosition;layout(location=1) in vec3 aNormal;layout(location=2) in vec2 aUv;
uniform mat4 uMvp;uniform mat4 uModel;out vec3 vNormal;out vec2 vUv;
void main(){gl_Position=uMvp*vec4(aPosition,1.0);vNormal=mat3(uModel)*aNormal;vUv=aUv;})";
const char* meshFragment=R"(#version 300 es
precision highp float;in vec3 vNormal;in vec2 vUv;out vec4 outColor;
uniform vec4 uBaseColor;uniform sampler2D uBaseTexture;uniform int uHasTexture;
uniform float uMetallic;uniform float uRoughness;uniform int uSelected;
void main(){
 vec3 n=normalize(vNormal);vec3 l=normalize(vec3(-0.4,0.8,0.5));
 vec3 v=normalize(vec3(0.0,0.0,1.0));vec3 h=normalize(l+v);
 vec4 albedo=uBaseColor;if(uHasTexture==1)albedo*=texture(uBaseTexture,vUv);
 float ndl=max(dot(n,l),0.0);float rough=clamp(uRoughness,0.08,1.0);float metal=clamp(uMetallic,0.0,1.0);
 float spec=pow(max(dot(n,h),0.0),mix(128.0,4.0,rough))*(1.0-rough*0.65);
 vec3 f0=mix(vec3(0.04),albedo.rgb,metal);
 float ambient=0.62;float directional=0.34*ndl;float fill=0.12*max(n.y,0.0)+0.08*max(-n.y,0.0);
 vec3 color=albedo.rgb*(ambient+directional+fill)*(1.0-metal)+f0*spec*max(ndl,0.18);
 color=color/(color+vec3(1.0));color=pow(max(color,vec3(0.0)),vec3(1.0/2.2));
 if(uSelected==1)color=mix(color,vec3(0.18,0.72,1.0),0.16);
 outColor=vec4(color,albedo.a);
})";
const char* lineVertex=R"(#version 300 es
layout(location=0) in vec3 aPosition;layout(location=1) in vec3 aColor;
uniform mat4 uMvp;out vec3 vColor;void main(){gl_Position=uMvp*vec4(aPosition,1.0);vColor=aColor;})";
const char* lineFragment=R"(#version 300 es
precision mediump float;in vec3 vColor;out vec4 outColor;void main(){outColor=vec4(vColor,1.0);})";
using Vertex=std::array<float,6>;
std::vector<float> BuildCubeVertices(){
 // Four independent vertices per face guarantee a constant outward normal on each face.
 struct Face{Vec3 a,b,c,d,n;};
 const Face faces[]={
  {{-1,-1, 1},{ 1,-1, 1},{ 1, 1, 1},{-1, 1, 1},{ 0, 0, 1}}, // +Z
  {{ 1,-1,-1},{-1,-1,-1},{-1, 1,-1},{ 1, 1,-1},{ 0, 0,-1}}, // -Z
  {{ 1,-1, 1},{ 1,-1,-1},{ 1, 1,-1},{ 1, 1, 1},{ 1, 0, 0}}, // +X
  {{-1,-1,-1},{-1,-1, 1},{-1, 1, 1},{-1, 1,-1},{-1, 0, 0}}, // -X
  {{-1, 1, 1},{ 1, 1, 1},{ 1, 1,-1},{-1, 1,-1},{ 0, 1, 0}}, // +Y
  {{-1,-1,-1},{ 1,-1,-1},{ 1,-1, 1},{-1,-1, 1},{ 0,-1, 0}}  // -Y
 };
 constexpr int indices[6]={0,1,2,0,2,3};
 std::vector<float> out;out.reserve(36*6);
 for(const Face& face:faces){
  const Vec3 corners[4]={face.a,face.b,face.c,face.d};
  for(int index:indices){const Vec3 p=corners[index];out.insert(out.end(),{p.x,p.y,p.z,face.n.x,face.n.y,face.n.z});}
 }
 return out;
}
bool ProjectPoint(const Mat4& m,Vec3 p,int width,int height,float&sx,float&sy){
 const float x=m.m[0]*p.x+m.m[4]*p.y+m.m[8]*p.z+m.m[12];
 const float y=m.m[1]*p.x+m.m[5]*p.y+m.m[9]*p.z+m.m[13];
 const float w=m.m[3]*p.x+m.m[7]*p.y+m.m[11]*p.z+m.m[15];
 if(w<=1e-5f)return false;const float nx=x/w,ny=y/w;
 sx=(nx*.5f+.5f)*width;sy=(.5f-ny*.5f)*height;
 return std::isfinite(sx)&&std::isfinite(sy);
}
float SegmentDistanceSquared(float px,float py,float ax,float ay,float bx,float by,float& t){
 const float dx=bx-ax,dy=by-ay,len=dx*dx+dy*dy;
 t=len>1e-5f?std::clamp(((px-ax)*dx+(py-ay)*dy)/len,0.0f,1.0f):0.0f;
 const float ex=px-(ax+t*dx),ey=py-(ay+t*dy);return ex*ex+ey*ey;
}
}}
namespace aster{
Mat4 OpenGLES3Renderer::ViewProjection()const{
 Mat4 projection=Mat4::Perspective(.82f,float(width_)/float(std::max(1,height_)),.1f,100.0f);
 Mat4 view=Mat4::LookAt(engine_->CameraPosition(),engine_->CameraTarget(),{0,1,0});
 return projection*view;
}
void OpenGLES3Renderer::Initialize(){
 program_=LinkProgram(meshVertex,meshFragment);lineProgram_=LinkProgram(lineVertex,lineFragment);
 if(!program_||!lineProgram_)return;
 mvp_=glGetUniformLocation(program_,"uMvp");model_=glGetUniformLocation(program_,"uModel");baseColor_=glGetUniformLocation(program_,"uBaseColor");baseTexture_=glGetUniformLocation(program_,"uBaseTexture");hasTexture_=glGetUniformLocation(program_,"uHasTexture");metallic_=glGetUniformLocation(program_,"uMetallic");roughness_=glGetUniformLocation(program_,"uRoughness");selected_=glGetUniformLocation(program_,"uSelected");glUniform1i(baseTexture_,0);glUniform4f(baseColor_,.16f,.48f,.68f,1);glUniform1i(hasTexture_,0);glUniform1f(metallic_,0.0f);glUniform1f(roughness_,0.72f);glUniform1i(selected_,0);
 lineMvp_=glGetUniformLocation(lineProgram_,"uMvp");
 const std::vector<float> vertices=BuildCubeVertices();
 glGenVertexArrays(1,&vao_);glGenBuffers(1,&vbo_);
 glBindVertexArray(vao_);glBindBuffer(GL_ARRAY_BUFFER,vbo_);
 glBufferData(GL_ARRAY_BUFFER,static_cast<GLsizeiptr>(vertices.size()*sizeof(float)),vertices.data(),GL_STATIC_DRAW);
 glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,GL_FALSE,6*sizeof(float),nullptr);
 glEnableVertexAttribArray(1);glVertexAttribPointer(1,3,GL_FLOAT,GL_FALSE,6*sizeof(float),reinterpret_cast<void*>(3*sizeof(float)));
 glBindVertexArray(0);
 glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LESS);glDepthMask(GL_TRUE);
 glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glFrontFace(GL_CCW);
 glClearDepthf(1.0f);
}
void OpenGLES3Renderer::Resize(int width,int height){
 width_=width>0?width:1;height_=height>0?height:1;glViewport(0,0,width_,height_);
}
int OpenGLES3Renderer::PickGizmoAxis(float x,float y)const{
 if(!scene_||!engine_||!engine_->Selected())return 0;
 const Entity*entity=scene_->Find(engine_->Selected());if(!entity)return 0;
 const Mat4 vp=ViewProjection();const Vec3 origin=entity->transform.position;
 const Vec3 axes[3]={{1,0,0},{0,1,0},{0,0,1}};
 float best=22.0f*22.0f;int selectedAxis=0;
 for(int i=0;i<3;++i){float x0,y0,x1,y1;
  if(!ProjectPoint(vp,origin,width_,height_,x0,y0)||
     !ProjectPoint(vp,origin+axes[i]*1.5f,width_,height_,x1,y1))continue;
  float t=0;const float d=SegmentDistanceSquared(x,y,x0,y0,x1,y1,t);
  if(t>=0.08f&&d<best){best=d;selectedAxis=i+1;}
 }
 return selectedAxis;
}
int OpenGLES3Renderer::PickEntity(float x,float y)const{
 if(!scene_||!engine_)return 0;
 const Mat4 vp=ViewProjection();float best=std::numeric_limits<float>::max();int picked=0;
 for(const Entity& e:scene_->Entities()){
  if(!e.visible||e.kind==EntityKind::Light||e.kind==EntityKind::Camera)continue;
  float sx=0,sy=0,edgeX=0,edgeY=0;
  const float worldRadius=std::max({std::fabs(e.transform.scale.x),std::fabs(e.transform.scale.y),std::fabs(e.transform.scale.z)});
  if(!ProjectPoint(vp,e.transform.position,width_,height_,sx,sy)||
     !ProjectPoint(vp,e.transform.position+Vec3{worldRadius,0,0},width_,height_,edgeX,edgeY))continue;
  const float radius=std::max(22.0f,std::hypot(edgeX-sx,edgeY-sy)+10.0f);
  const float dx=x-sx,dy=y-sy,d=dx*dx+dy*dy;
  if(d<=radius*radius&&d<best){best=d;picked=static_cast<int>(e.id);}
 }
 return picked;
}
void OpenGLES3Renderer::DragGizmoAxis(int axis,float dx,float dy){
 if(axis<1||axis>3||!scene_||!engine_)return;
 const Entity*entity=scene_->Find(engine_->Selected());if(!entity)return;
 const Vec3 origin=entity->transform.position;
 const Vec3 direction=axis==1?Vec3{1,0,0}:axis==2?Vec3{0,1,0}:Vec3{0,0,1};
 float x0,y0,x1,y1;const Mat4 vp=ViewProjection();
 if(!ProjectPoint(vp,origin,width_,height_,x0,y0)||
    !ProjectPoint(vp,origin+direction*1.5f,width_,height_,x1,y1))return;
 const float vx=x1-x0,vy=y1-y0,pixels=std::sqrt(vx*vx+vy*vy);
 if(pixels<2.0f)return;
 const float amount=(dx*vx+dy*vy)/(pixels*pixels)*1.5f;
 engine_->DragSelectedAxis(axis,amount);
}
void OpenGLES3Renderer::DrawLines(const Mat4&vp){
 std::vector<float> data;
 auto line=[&](Vec3 a,Vec3 b,Vec3 c){for(Vec3 p:{a,b})data.insert(data.end(),{p.x,p.y,p.z,c.x,c.y,c.z});};
 for(int i=-20;i<=20;++i){float q=float(i);Vec3 c=i==0?Vec3{.3f,.4f,.5f}:Vec3{.15f,.21f,.28f};line({q,0,-20},{q,0,20},c);line({-20,0,q},{20,0,q},c);}
 const Entity*e=scene_->Find(engine_->Selected());const Vec3 o=e?e->transform.position:Vec3{};
 line(o,o+Vec3{1.5f,0,0},{1,.12f,.12f});line(o,o+Vec3{0,1.5f,0},{.12f,1,.2f});line(o,o+Vec3{0,0,1.5f},{.15f,.45f,1});
 line({-3,2,-3},{-2.5f,2,-3},{1,.75f,.15f});line({-3,2,-3},{-3,2.5f,-3},{1,.75f,.15f});
 GLuint va=0,vb=0;glGenVertexArrays(1,&va);glGenBuffers(1,&vb);glBindVertexArray(va);glBindBuffer(GL_ARRAY_BUFFER,vb);
 glBufferData(GL_ARRAY_BUFFER,static_cast<GLsizeiptr>(data.size()*sizeof(float)),data.data(),GL_STREAM_DRAW);
 glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,GL_FALSE,6*sizeof(float),nullptr);
 glEnableVertexAttribArray(1);glVertexAttribPointer(1,3,GL_FLOAT,GL_FALSE,6*sizeof(float),reinterpret_cast<void*>(3*sizeof(float)));
 glUseProgram(lineProgram_);glUniformMatrix4fv(lineMvp_,1,GL_FALSE,vp.m);
 glDisable(GL_DEPTH_TEST);glDrawArrays(GL_LINES,0,static_cast<GLsizei>(data.size()/6));glEnable(GL_DEPTH_TEST);
 glBindVertexArray(0);glDeleteBuffers(1,&vb);glDeleteVertexArrays(1,&va);
}
void OpenGLES3Renderer::RenderFrame(){
 glClearColor(.045f,.075f,.105f,1.0f);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
 if(!scene_||!engine_||!program_)return;
 glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LESS);glDepthMask(GL_TRUE);glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glFrontFace(GL_CCW);
 const Mat4 vp=ViewProjection();glUseProgram(program_);glBindVertexArray(vao_);
 for(const Entity&e:scene_->Entities()){
  if(!e.visible||e.kind==EntityKind::Light||e.kind==EntityKind::Camera)continue;
  const Mat4 model=Mat4::Translation(e.transform.position)*Mat4::RotationZ(e.transform.rotation.z)*Mat4::RotationY(e.transform.rotation.y)*Mat4::RotationX(e.transform.rotation.x)*Mat4::Scale(e.transform.scale);
  const Mat4 mvp=vp*model;glUniformMatrix4fv(mvp_,1,GL_FALSE,mvp.m);glUniformMatrix4fv(model_,1,GL_FALSE,model.m);
  glUniform1i(selected_,e.id==engine_->Selected()?1:0);glUniform1f(metallic_,0.0f);glUniform1f(roughness_,0.72f);glDrawArrays(GL_TRIANGLES,0,36);
 }
 glBindVertexArray(0);
 for(const auto& model:imported_){const Entity* owner=scene_->Find(model->entityId);if(!owner||!owner->visible)continue;const Mat4 transform=Mat4::Translation(owner->transform.position)*Mat4::RotationZ(owner->transform.rotation.z)*Mat4::RotationY(owner->transform.rotation.y)*Mat4::RotationX(owner->transform.rotation.x)*Mat4::Scale(owner->transform.scale);const Mat4 modelMvp=vp*transform;for(const auto& primitive:model->primitives){glUseProgram(program_);glBindVertexArray(primitive.vao);glUniformMatrix4fv(mvp_,1,GL_FALSE,modelMvp.m);glUniformMatrix4fv(model_,1,GL_FALSE,transform.m);
  ImportedMaterial mat;if(primitive.materialIndex>=0&&static_cast<size_t>(primitive.materialIndex)<model->materials.size())mat=model->materials[primitive.materialIndex];
  glUniform4fv(baseColor_,1,mat.baseColor);glUniform1i(hasTexture_,mat.baseColorTexture>=0?1:0);glUniform1f(metallic_,mat.metallic);glUniform1f(roughness_,mat.roughness);glUniform1i(selected_,0);model->assets.BindBaseColor(mat,0);
  glUniform1i(selected_,owner->id==engine_->Selected()?1:0);glDrawElements(GL_TRIANGLES,primitive.indexCount,GL_UNSIGNED_INT,nullptr);
  }
 }
 glBindVertexArray(0);DrawLines(vp);
}
bool OpenGLES3Renderer::ImportModel(const std::string& path,EntityId entityId,std::string& error){
 ImportResult parsed=GltfImporter::Load(path);if(!parsed){error=parsed.error;return false;}
 auto gpu=std::make_unique<GpuModel>();gpu->entityId=entityId;gpu->materials=parsed.model.materials;
 if(!gpu->assets.Upload(parsed.model)){error="تعذر رفع خامات النموذج";return false;}
 for(const auto& mesh:parsed.model.meshes)for(const auto& src:mesh.primitives){
  if(src.vertices.empty()||src.indices.empty())continue;
  std::vector<float> v;v.reserve(src.vertices.size()*8);
  for(const auto& x:src.vertices)v.insert(v.end(),{x.position[0],x.position[1],x.position[2],x.normal[0],x.normal[1],x.normal[2],x.uv[0],x.uv[1]});
  GpuPrimitive p;p.indexCount=static_cast<int>(src.indices.size());p.materialIndex=src.materialIndex;
  glGenVertexArrays(1,&p.vao);glGenBuffers(1,&p.vbo);glGenBuffers(1,&p.ebo);glBindVertexArray(p.vao);
  glBindBuffer(GL_ARRAY_BUFFER,p.vbo);glBufferData(GL_ARRAY_BUFFER,v.size()*sizeof(float),v.data(),GL_STATIC_DRAW);
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,p.ebo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,src.indices.size()*sizeof(uint32_t),src.indices.data(),GL_STATIC_DRAW);
  glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,GL_FALSE,8*sizeof(float),nullptr);
  glEnableVertexAttribArray(1);glVertexAttribPointer(1,3,GL_FLOAT,GL_FALSE,8*sizeof(float),reinterpret_cast<void*>(3*sizeof(float)));
  glEnableVertexAttribArray(2);glVertexAttribPointer(2,2,GL_FLOAT,GL_FALSE,8*sizeof(float),reinterpret_cast<void*>(6*sizeof(float)));
  glBindVertexArray(0);gpu->primitives.push_back(p);
 }
 if(gpu->primitives.empty()){error="لا يحتوي الملف على مجسمات مثلثية";return false;}imported_.push_back(std::move(gpu));error.clear();return true;
}
void OpenGLES3Renderer::Shutdown(){
 for(auto& m:imported_)for(auto& p:m->primitives){if(p.ebo)glDeleteBuffers(1,&p.ebo);if(p.vbo)glDeleteBuffers(1,&p.vbo);if(p.vao)glDeleteVertexArrays(1,&p.vao);}imported_.clear();if(vbo_)glDeleteBuffers(1,&vbo_);if(vao_)glDeleteVertexArrays(1,&vao_);
 if(sphereVbo_)glDeleteBuffers(1,&sphereVbo_);if(sphereVao_)glDeleteVertexArrays(1,&sphereVao_);
 if(program_)glDeleteProgram(program_);if(lineProgram_)glDeleteProgram(lineProgram_);
 vbo_=vao_=sphereVbo_=sphereVao_=program_=lineProgram_=0;sphereVertexCount_=0;
}
}
