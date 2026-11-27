#include "aster/EngineRuntime.hpp"
#include <utility>
namespace aster{
Mat4 Mat4::Identity(){Mat4 r{};r.m[0]=r.m[5]=r.m[10]=r.m[15]=1;return r;}
Mat4 Mat4::Perspective(float f,float a,float n,float z){Mat4 r{};float q=1/std::tan(f*.5f);r.m[0]=q/a;r.m[5]=q;r.m[10]=(z+n)/(n-z);r.m[11]=-1;r.m[14]=2*z*n/(n-z);return r;}
Mat4 Mat4::LookAt(Vec3 e,Vec3 c,Vec3 up){Vec3 f=(c-e).Normalized(),s=Vec3::Cross(f,up).Normalized(),u=Vec3::Cross(s,f);Mat4 r=Identity();r.m[0]=s.x;r.m[4]=s.y;r.m[8]=s.z;r.m[1]=u.x;r.m[5]=u.y;r.m[9]=u.z;r.m[2]=-f.x;r.m[6]=-f.y;r.m[10]=-f.z;r.m[12]=-Vec3::Dot(s,e);r.m[13]=-Vec3::Dot(u,e);r.m[14]=Vec3::Dot(f,e);return r;}
Mat4 Mat4::Translation(Vec3 p){Mat4 r=Identity();r.m[12]=p.x;r.m[13]=p.y;r.m[14]=p.z;return r;}
Mat4 Mat4::Scale(Vec3 s){Mat4 r=Identity();r.m[0]=s.x;r.m[5]=s.y;r.m[10]=s.z;return r;}
Mat4 Mat4::RotationX(float a){Mat4 r=Identity();float c=std::cos(a),s=std::sin(a);r.m[5]=c;r.m[6]=s;r.m[9]=-s;r.m[10]=c;return r;}
Mat4 Mat4::RotationY(float a){Mat4 r=Identity();float c=std::cos(a),s=std::sin(a);r.m[0]=c;r.m[2]=-s;r.m[8]=s;r.m[10]=c;return r;}
Mat4 Mat4::RotationZ(float a){Mat4 r=Identity();float c=std::cos(a),s=std::sin(a);r.m[0]=c;r.m[1]=s;r.m[4]=-s;r.m[5]=c;return r;}
Mat4 operator*(const Mat4&a,const Mat4&b){Mat4 r{};for(int c=0;c<4;c++)for(int y=0;y<4;y++)for(int k=0;k<4;k++)r.m[c*4+y]+=a.m[k*4+y]*b.m[c*4+k];return r;}
EntityId Scene::Add(EntityKind k,Vec3 p){
 Entity e{};e.id=nextId_++;e.kind=k;e.transform.position=p;
 if(k==EntityKind::Light||k==EntityKind::Camera){e.body.isStatic=true;e.body.useGravity=false;}
 entities_.push_back(e);return e.id;
}
Entity* Scene::Find(EntityId id){for(auto& e:entities_)if(e.id==id)return &e;return nullptr;}
const Entity* Scene::Find(EntityId id)const{for(const auto&e:entities_)if(e.id==id)return &e;return nullptr;}
std::uint32_t AssetRegistry::Register(std::string n,std::string t){auto i=records_.find(n);if(i!=records_.end())return i->second.id;auto id=next_++;records_.emplace(n,AssetRecord{id,std::move(n),std::move(t)});return id;}
const AssetRecord* AssetRegistry::Find(const std::string&n)const{auto i=records_.find(n);return i==records_.end()?nullptr:&i->second;}
bool PhysicsWorld::IsBody(const Entity&e){return e.kind==EntityKind::Cube||e.kind==EntityKind::Sphere;}
Vec3 PhysicsWorld::HalfExtents(const Entity&e){
 return {std::max(.025f,std::fabs(e.transform.scale.x)),
         std::max(.025f,std::fabs(e.transform.scale.y)),
         std::max(.025f,std::fabs(e.transform.scale.z))};
}
void PhysicsWorld::ResolveGround(Entity&e){
 if(!IsBody(e))return;
 const Vec3 half=HalfExtents(e);
 const float bottom=e.transform.position.y-half.y;
 if(bottom<0.0f){
  e.transform.position.y=half.y;
  if(e.body.velocity.y<0.0f)e.body.velocity.y=-e.body.velocity.y*e.body.restitution;
  if(std::fabs(e.body.velocity.y)<0.12f)e.body.velocity.y=0.0f;
  const float damping=std::clamp(1.0f-e.body.friction*0.08f,0.0f,1.0f);
  e.body.velocity.x*=damping;e.body.velocity.z*=damping;
  e.body.grounded=true;
 }else e.body.grounded=false;
}
void PhysicsWorld::ResolvePair(Entity&a,Entity&b){
 if(!IsBody(a)||!IsBody(b))return;
 const Vec3 ha=HalfExtents(a),hb=HalfExtents(b);
 const Vec3 delta=b.transform.position-a.transform.position;
 const Vec3 overlap={ha.x+hb.x-std::fabs(delta.x),ha.y+hb.y-std::fabs(delta.y),ha.z+hb.z-std::fabs(delta.z)};
 if(overlap.x<=0.0f||overlap.y<=0.0f||overlap.z<=0.0f)return;
 const float invA=a.body.isStatic?0.0f:1.0f/std::max(a.body.mass,0.001f);
 const float invB=b.body.isStatic?0.0f:1.0f/std::max(b.body.mass,0.001f);
 const float invSum=invA+invB;if(invSum<=0.0f)return;
 Vec3 normal{};float penetration=overlap.x;
 if(overlap.y<penetration){penetration=overlap.y;normal={0,delta.y>=0?1.0f:-1.0f,0};}
 else normal={delta.x>=0?1.0f:-1.0f,0,0};
 if(overlap.z<penetration){penetration=overlap.z;normal={0,0,delta.z>=0?1.0f:-1.0f};}
 constexpr float slop=0.001f;constexpr float correction=0.82f;
 const Vec3 correctionVector=normal*(std::max(penetration-slop,0.0f)*correction/invSum);
 if(invA>0.0f)a.transform.position+=correctionVector*(-invA);
 if(invB>0.0f)b.transform.position+=correctionVector*invB;
 const Vec3 relative=b.body.velocity-a.body.velocity;
 const float alongNormal=Vec3::Dot(relative,normal);
 if(alongNormal>=0.0f)return;
 const float restitution=std::min(a.body.restitution,b.body.restitution);
 const float impulseMagnitude=-(1.0f+restitution)*alongNormal/invSum;
 const Vec3 impulse=normal*impulseMagnitude;
 if(invA>0.0f)a.body.velocity+=impulse*(-invA);
 if(invB>0.0f)b.body.velocity+=impulse*invB;
 const Vec3 tangentVelocity=relative-normal*alongNormal;
 const Vec3 tangent=tangentVelocity.Normalized();
 if(Vec3::Dot(tangent,tangent)>0.5f){
  float jt=-Vec3::Dot(relative,tangent)/invSum;
  const float mu=std::sqrt(std::max(0.0f,a.body.friction*b.body.friction));
  jt=std::clamp(jt,-impulseMagnitude*mu,impulseMagnitude*mu);
  const Vec3 frictionImpulse=tangent*jt;
  if(invA>0.0f)a.body.velocity+=frictionImpulse*(-invA);
  if(invB>0.0f)b.body.velocity+=frictionImpulse*invB;
 }
}
void PhysicsWorld::Step(Scene&s,float dt){
 if(!(dt>0.0f)||!std::isfinite(dt))return;
 auto& entities=s.Entities();
 for(auto&e:entities){
  if(!IsBody(e)||e.body.isStatic)continue;
  e.body.grounded=false;
  if(e.body.useGravity)e.body.velocity.y-=9.81f*dt;
  e.transform.position+=e.body.velocity*dt;
  ResolveGround(e);
 }
 for(std::size_t i=0;i<entities.size();++i)
  for(std::size_t j=i+1;j<entities.size();++j)
   ResolvePair(entities[i],entities[j]);
 for(auto&e:entities)if(IsBody(e)&&!e.body.isStatic)ResolveGround(e);
}
void Engine::Start(){
 if(running_)return;
 startState_.clear();
 for(const auto&e:scene_.Entities())startState_.emplace(e.id,StartState{e.transform,e.body.velocity});
 running_=true;accumulator_=0;frames_=0;
 assets_.Register("Cube_Mesh","Mesh");assets_.Register("Default_Lit","Material");
}
void Engine::Stop(){
 if(!running_)return;
 running_=false;accumulator_=0;
 for(auto&e:scene_.Entities()){
  auto i=startState_.find(e.id);
  if(i!=startState_.end()){e.transform=i->second.transform;e.body.velocity=i->second.velocity;e.body.grounded=false;}
 }
 startState_.clear();
}
void Engine::Tick(double d){
 if(!running_)return;if(!std::isfinite(d)||d<0)d=0;
 accumulator_+=std::min(d,.25);
 int steps=0;constexpr int maxSubsteps=8;
 while(accumulator_>=fixedStep_&&steps<maxSubsteps){
  FixedUpdate(static_cast<float>(fixedStep_));accumulator_-=fixedStep_;++steps;
 }
 if(steps==maxSubsteps)accumulator_=std::min(accumulator_,fixedStep_);
 ++frames_;
}
void Engine::FixedUpdate(float dt){physics_.Step(scene_,dt);}
Vec3 Engine::CameraPosition()const{float cp=std::cos(cameraPitch_);return cameraTarget_+Vec3{std::sin(cameraYaw_)*cp,std::sin(cameraPitch_),std::cos(cameraYaw_)*cp}*cameraDistance_;}
void Engine::Orbit(float dx,float dy){cameraYaw_-=dx*.008f;cameraPitch_=std::clamp(cameraPitch_+dy*.008f,-1.35f,1.35f);}
void Engine::Pan(float dx,float dy){Vec3 forward=(cameraTarget_-CameraPosition()).Normalized();Vec3 right=Vec3::Cross(forward,{0,1,0}).Normalized();Vec3 up=Vec3::Cross(right,forward).Normalized();float s=cameraDistance_*.0018f;cameraTarget_+=right*(-dx*s)+up*(dy*s);}
void Engine::Zoom(float factor){if(factor>0&&std::isfinite(factor))cameraDistance_=std::clamp(cameraDistance_*factor,1.5f,35.0f);}
void Engine::DragSelected(float dx,float dy){auto*e=scene_.Find(selected_);if(!e)return;Vec3 f=(cameraTarget_-CameraPosition()).Normalized();Vec3 r=Vec3::Cross(f,{0,1,0}).Normalized();Vec3 u=Vec3::Cross(r,f).Normalized();float s=cameraDistance_*.002f;e->transform.position+=r*(dx*s)+u*(-dy*s);cameraTarget_+=r*(dx*s)+u*(-dy*s);}
void Engine::DragSelectedAxis(int axis,float amount){auto*e=scene_.Find(selected_);if(!e||!std::isfinite(amount))return;if(axis==1)e->transform.position.x+=amount;else if(axis==2)e->transform.position.y+=amount;else if(axis==3)e->transform.position.z+=amount;}
void Engine::SetStatic(EntityId id,bool value){if(auto*e=scene_.Find(id)){e->body.isStatic=value;if(value)e->body.velocity={};}}
void Engine::SetVelocity(EntityId id,Vec3 v){if(auto*e=scene_.Find(id);e&&!e->body.isStatic)e->body.velocity=v;}
Vec3 Engine::Velocity(EntityId id)const{const auto*e=scene_.Find(id);return e?e->body.velocity:Vec3{};}
void Engine::SetPosition(EntityId id,Vec3 p){if(auto*e=scene_.Find(id))e->transform.position=p;}
void Engine::SetScale(EntityId id,Vec3 s){if(auto*e=scene_.Find(id))e->transform.scale={std::max(.05f,s.x),std::max(.05f,s.y),std::max(.05f,s.z)};}
}
