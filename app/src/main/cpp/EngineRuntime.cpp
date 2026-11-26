#include "aster/EngineRuntime.hpp"
#include <utility>
namespace aster{
Mat4 Mat4::Identity(){Mat4 r{};r.m[0]=r.m[5]=r.m[10]=r.m[15]=1;return r;}
Mat4 Mat4::Perspective(float f,float a,float n,float z){Mat4 r{};float q=1/std::tan(f*.5f);r.m[0]=q/a;r.m[5]=q;r.m[10]=(z+n)/(n-z);r.m[11]=-1;r.m[14]=2*z*n/(n-z);return r;}
Mat4 Mat4::LookAt(Vec3 e,Vec3 c,Vec3 up){Vec3 f=(c-e).Normalized(),s=Vec3::Cross(f,up).Normalized(),u=Vec3::Cross(s,f);Mat4 r=Identity();r.m[0]=s.x;r.m[4]=s.y;r.m[8]=s.z;r.m[1]=u.x;r.m[5]=u.y;r.m[9]=u.z;r.m[2]=-f.x;r.m[6]=-f.y;r.m[10]=-f.z;r.m[12]=-Vec3::Dot(s,e);r.m[13]=-Vec3::Dot(u,e);r.m[14]=Vec3::Dot(f,e);return r;}
Mat4 Mat4::Translation(Vec3 p){Mat4 r=Identity();r.m[12]=p.x;r.m[13]=p.y;r.m[14]=p.z;return r;}
Mat4 Mat4::Scale(Vec3 s){Mat4 r=Identity();r.m[0]=s.x;r.m[5]=s.y;r.m[10]=s.z;return r;}
Mat4 Mat4::RotationY(float a){Mat4 r=Identity();float c=std::cos(a),s=std::sin(a);r.m[0]=c;r.m[2]=-s;r.m[8]=s;r.m[10]=c;return r;}
Mat4 operator*(const Mat4&a,const Mat4&b){Mat4 r{};for(int c=0;c<4;c++)for(int y=0;y<4;y++)for(int k=0;k<4;k++)r.m[c*4+y]+=a.m[k*4+y]*b.m[c*4+k];return r;}
EntityId Scene::Add(EntityKind k,Vec3 p){Entity e{};e.id=nextId_++;e.kind=k;e.transform.position=p;entities_.push_back(e);return e.id;}
Entity* Scene::Find(EntityId id){for(auto& e:entities_)if(e.id==id)return &e;return nullptr;}
const Entity* Scene::Find(EntityId id)const{for(const auto&e:entities_)if(e.id==id)return &e;return nullptr;}
std::uint32_t AssetRegistry::Register(std::string n,std::string t){auto i=records_.find(n);if(i!=records_.end())return i->second.id;auto id=next_++;records_.emplace(n,AssetRecord{id,std::move(n),std::move(t)});return id;}
const AssetRecord* AssetRegistry::Find(const std::string&n)const{auto i=records_.find(n);return i==records_.end()?nullptr:&i->second;}
void PhysicsWorld::Step(Scene&s,float dt){for(auto&e:s.Entities()){if(e.kind==EntityKind::Cube||e.kind==EntityKind::Sphere){e.spin+=dt*.55f;e.transform.rotation.y=e.spin;if(e.transform.position.y>0){e.transform.position.y=std::max(0.0f,e.transform.position.y-9.81f*dt*dt*.5f);}}}}
void Engine::Start(){if(running_)return;running_=true;accumulator_=0;frames_=0;assets_.Register("Cube_Mesh","Mesh");assets_.Register("Default_Lit","Material");}
void Engine::Stop(){running_=false;}
void Engine::Tick(double d){if(!running_)return;if(!std::isfinite(d)||d<0)d=0;accumulator_+=std::min(d,.25);while(accumulator_>=fixedStep_){FixedUpdate(static_cast<float>(fixedStep_));accumulator_-=fixedStep_;}++frames_;}
void Engine::FixedUpdate(float dt){physics_.Step(scene_,dt);}
Vec3 Engine::CameraPosition()const{float cp=std::cos(cameraPitch_);return cameraTarget_+Vec3{std::sin(cameraYaw_)*cp,std::sin(cameraPitch_),std::cos(cameraYaw_)*cp}*cameraDistance_;}
void Engine::Orbit(float dx,float dy){cameraYaw_-=dx*.008f;cameraPitch_=std::clamp(cameraPitch_+dy*.008f,-1.35f,1.35f);}
void Engine::Pan(float dx,float dy){Vec3 forward=(cameraTarget_-CameraPosition()).Normalized();Vec3 right=Vec3::Cross(forward,{0,1,0}).Normalized();Vec3 up=Vec3::Cross(right,forward).Normalized();float s=cameraDistance_*.0018f;cameraTarget_+=right*(-dx*s)+up*(dy*s);}
void Engine::Zoom(float factor){if(factor>0&&std::isfinite(factor))cameraDistance_=std::clamp(cameraDistance_*factor,1.5f,35.0f);}
void Engine::DragSelected(float dx,float dy){auto*e=scene_.Find(selected_);if(!e)return;Vec3 f=(cameraTarget_-CameraPosition()).Normalized();Vec3 r=Vec3::Cross(f,{0,1,0}).Normalized();Vec3 u=Vec3::Cross(r,f).Normalized();float s=cameraDistance_*.002f;e->transform.position+=r*(dx*s)+u*(-dy*s);cameraTarget_+=r*(dx*s)+u*(-dy*s);}
void Engine::SetPosition(EntityId id,Vec3 p){if(auto*e=scene_.Find(id))e->transform.position=p;}
void Engine::SetScale(EntityId id,Vec3 s){if(auto*e=scene_.Find(id))e->transform.scale={std::max(.05f,s.x),std::max(.05f,s.y),std::max(.05f,s.z)};}
}
