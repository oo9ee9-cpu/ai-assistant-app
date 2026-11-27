#include "aster/EngineRuntime.hpp"
#include <cassert>
#include <cmath>
using namespace aster;
static void Step(Engine&e,int frames){for(int i=0;i<frames;++i)e.Tick(1.0/60.0);}
int main(){
 Engine e;
 const auto floor=e.GetScene().Add(EntityKind::Cube,{0,-1,0});
 e.SetScale(floor,{8,1,8});e.SetStatic(floor,true);
 const auto falling=e.GetScene().Add(EntityKind::Cube,{0,4,0});
 e.Start();Step(e,120);
 const Entity* landed=e.GetScene().Find(falling);
 assert(landed->transform.position.y>=0.99f&&landed->transform.position.y<=1.02f);
 assert(std::fabs(landed->body.velocity.y)<0.2f);
 const float beforeStop=landed->transform.position.y;
 e.Stop();assert(!e.IsRunning());
 assert(std::fabs(e.GetScene().Find(falling)->transform.position.y-4.0f)<1e-5f);
 assert(beforeStop<4.0f);

 // A dynamic cube collides with a static cube and does not pass through it.
 Engine collision;
 const auto wall=collision.GetScene().Add(EntityKind::Cube,{0,1,0});
 collision.SetStatic(wall,true);
 const auto mover=collision.GetScene().Add(EntityKind::Cube,{-2,1,0});
 collision.SetVelocity(mover,{5,0,0});
 collision.Start();Step(collision,60);
 const Entity* moving=collision.GetScene().Find(mover);
 assert(moving->transform.position.x<=-0.98f);
 assert(moving->body.velocity.x<0.1f);
 collision.Stop();
 assert(std::fabs(collision.GetScene().Find(mover)->transform.position.x+2.0f)<1e-5f);

 // Existing runtime and axis drag behavior remain intact.
 Engine input;const auto id=input.GetScene().Add(EntityKind::Cube,{0,1,0});
 input.Select(id);input.SetPosition(id,{1,2,3});input.SetScale(id,{2,2,2});
 input.DragSelectedAxis(1,1.25f);auto*p=input.GetScene().Find(id);
 assert(std::fabs(p->transform.position.x-2.25f)<1e-5f);
 input.DragSelectedAxis(2,-.5f);assert(std::fabs(p->transform.position.y-1.5f)<1e-5f);
 input.DragSelectedAxis(3,2.0f);assert(std::fabs(p->transform.position.z-5.0f)<1e-5f);
 input.Start();input.Tick(1.0/30.0);assert(input.FrameCount()==1);input.Stop();

 // Sphere entities participate in the same dynamic-body simulation.
 Engine sphereScene;
 const auto sphereFloor=sphereScene.GetScene().Add(EntityKind::Cube,{0,-1,0});
 sphereScene.SetScale(sphereFloor,{6,1,6});sphereScene.SetStatic(sphereFloor,true);
 const auto sphere=sphereScene.GetScene().Add(EntityKind::Sphere,{0,3,0});
 sphereScene.Start();Step(sphereScene,90);
 const Entity* sphereBody=sphereScene.GetScene().Find(sphere);
 assert(sphereBody&&sphereBody->kind==EntityKind::Sphere);
 assert(sphereBody->transform.position.y>=0.95f);
 sphereScene.Stop();

 // Physics parameters are retained on the selected entity.
 Engine properties;
 const auto body=properties.GetScene().Add(EntityKind::Sphere,{1,2,3});
 properties.SetStatic(body,true);
 auto* bodyData=properties.GetScene().Find(body);
 bodyData->body.mass=3.5f;bodyData->body.friction=.42f;bodyData->body.restitution=.3f;bodyData->body.useGravity=false;
 assert(bodyData->body.isStatic&&std::fabs(bodyData->body.mass-3.5f)<1e-5f);
 assert(std::fabs(bodyData->body.friction-.42f)<1e-5f&&std::fabs(bodyData->body.restitution-.3f)<1e-5f);
 assert(!bodyData->body.useGravity);
}