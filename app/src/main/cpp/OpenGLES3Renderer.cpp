#include "aster/OpenGLES3Renderer.hpp"
#include <GLES3/gl3.h>
#include <array>
#include <vector>
namespace aster {
namespace {
GLuint Compile(GLenum type,const char* src){GLuint s=glCreateShader(type);glShaderSource(s,1,&src,nullptr);glCompileShader(s);GLint ok=0;glGetShaderiv(s,GL_COMPILE_STATUS,&ok);if(!ok){glDeleteShader(s);return 0;}return s;}
const char* vs=R"(#version 300 es
layout(location=0)in vec3 p;layout(location=1)in vec3 n;uniform mat4 uMvp;uniform mat4 uModel;out vec3 normal;
void main(){gl_Position=uMvp*vec4(p,1.0);normal=mat3(uModel)*n;})";
const char* fs=R"(#version 300 es
precision mediump float;in vec3 normal;out vec4 color;
void main(){vec3 l=normalize(vec3(-.4,.8,.5));float d=max(dot(normalize(normal),l),0.0);color=vec4(vec3(.16,.48,.68)*(.25+.75*d),1.0);})";
std::vector<float> Cube(){
 const float v[]={-1,-1,1,0,0,1, 1,-1,1,0,0,1, 1,1,1,0,0,1, -1,1,1,0,0,1,
 1,-1,-1,0,0,-1, -1,-1,-1,0,0,-1, -1,1,-1,0,0,-1, 1,1,-1,0,0,-1,
 1,-1,1,1,0,0, 1,-1,-1,1,0,0, 1,1,-1,1,0,0, 1,1,1,1,0,0,
 -1,-1,-1,-1,0,0, -1,-1,1,-1,0,0, -1,1,1,-1,0,0, -1,1,-1,-1,0,0,
 -1,1,1,0,1,0, 1,1,1,0,1,0, 1,1,-1,0,1,0, -1,1,-1,0,1,0,
 -1,-1,-1,0,-1,0, 1,-1,-1,0,-1,0, 1,-1,1,0,-1,0, -1,-1,1,0,-1,0};
 const int ix[]={0,1,2,0,2,3,4,5,6,4,6,7,8,9,10,8,10,11,12,13,14,12,14,15,16,17,18,16,18,19,20,21,22,20,22,23};
 std::vector<float> out;out.reserve(216);
 for(int i:ix)for(int k=0;k<6;k++)out.push_back(v[i*6+k]);
 return out;
}
}
void OpenGLES3Renderer::Initialize(){
 GLuint a=Compile(GL_VERTEX_SHADER,vs),b=Compile(GL_FRAGMENT_SHADER,fs);if(!a||!b)return;
 program_=glCreateProgram();glAttachShader(program_,a);glAttachShader(program_,b);glLinkProgram(program_);glDeleteShader(a);glDeleteShader(b);
 GLint ok=0;glGetProgramiv(program_,GL_LINK_STATUS,&ok);if(!ok){glDeleteProgram(program_);program_=0;return;}
 mvp_=glGetUniformLocation(program_,"uMvp");model_=glGetUniformLocation(program_,"uModel");
 auto data=Cube();glGenVertexArrays(1,&vao_);glGenBuffers(1,&vbo_);glBindVertexArray(vao_);glBindBuffer(GL_ARRAY_BUFFER,vbo_);
 glBufferData(GL_ARRAY_BUFFER,static_cast<GLsizeiptr>(data.size()*sizeof(float)),data.data(),GL_STATIC_DRAW);
 glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,GL_FALSE,6*sizeof(float),nullptr);
 glEnableVertexAttribArray(1);glVertexAttribPointer(1,3,GL_FLOAT,GL_FALSE,6*sizeof(float),reinterpret_cast<void*>(3*sizeof(float)));
 glBindVertexArray(0);glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);
}
void OpenGLES3Renderer::Resize(int w,int h){width_=w>0?w:1;height_=h>0?h:1;glViewport(0,0,width_,height_);}
void OpenGLES3Renderer::RenderFrame(){
 glClearColor(.045f,.075f,.105f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);if(!program_||!scene_)return;
 Mat4 proj=Mat4::Perspective(.82f,static_cast<float>(width_)/height_,.1f,100.f);
 Mat4 view=Mat4::LookAt({4.2f,3.2f,6.0f},{0,.2f,0},{0,1,0});Mat4 vp=proj*view;
 glUseProgram(program_);glBindVertexArray(vao_);
 for(const auto&e:scene_->Entities()){if(!e.visible)continue;Mat4 model=Mat4::Translation(e.transform.position)*Mat4::RotationY(e.transform.rotation.y)*Mat4::Scale(e.transform.scale);Mat4 m=vp*model;glUniformMatrix4fv(mvp_,1,GL_FALSE,m.m);glUniformMatrix4fv(model_,1,GL_FALSE,model.m);glDrawArrays(GL_TRIANGLES,0,36);}
 glBindVertexArray(0);
}
void OpenGLES3Renderer::Shutdown(){if(vbo_)glDeleteBuffers(1,&vbo_);if(vao_)glDeleteVertexArrays(1,&vao_);if(program_)glDeleteProgram(program_);vbo_=vao_=program_=0;}
}
