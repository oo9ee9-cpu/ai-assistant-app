#include "aster/EngineRuntime.hpp"
#include <cassert>
#include <cmath>
using namespace aster;
static void Step(Engine&e,int n){for(int i=0;i<n;++i)e.Tick(1.0/60.0);}
int main(){
 Engine e;auto floor=e.GetScene().Add(EntityKind::Cube,{0,-1,0});e.SetScale(floor,{8,1,8});e.SetStatic(floor,true);
 auto falling=e.GetScene().Add(EntityKind::Cube,{0,4,0});e.Start();Step(e,120);
 auto*landed=e.GetScene().Find(falling);assert(landed->transform.position.y>=.99f&&landed->transform.position.y<=1.02f);assert(std::fabs(landed->body.velocity.y)<.2f);e.Stop();assert(std::fabs(e.GetScene().Find(falling)->transform.position.y-4)<1e-5f);
 Engine col;auto wall=col.GetScene().Add(EntityKind::Cube,{0,1,0});col.SetStatic(wall,true);auto mover=col.GetScene().Add(EntityKind::Cube,{-2,1,0});col.SetVelocity(mover,{5,0,0});col.Start();Step(col,60);assert(col.GetScene().Find(mover)->transform.position.x<=-.98f);col.Stop();
 Engine p;auto id=p.GetScene().Add(EntityKind::Cube,{0,2,0});p.SetBodyProperties(id,-5,2,-1,false);p.SetVelocity(id,{1,2,3});auto*b=p.GetScene().Find(id);assert(std::fabs(b->body.mass-.001f)<1e-6f);assert(b->body.friction==1&&b->body.restitution==0&&!b->body.useGravity);p.SetStatic(id,true);assert(b->body.velocity.x==0);p.SetStatic(id,false);p.SetRotation(id,{.1f,.2f,.3f});assert(std::fabs(b->transform.rotation.y-.2f)<1e-6f);
 Engine input;auto q=input.GetScene().Add(EntityKind::Cube,{0,1,0});input.Select(q);input.SetPosition(q,{1,2,3});input.SetScale(q,{2,2,2});input.DragSelectedAxis(1,1.25f);assert(std::fabs(input.GetScene().Find(q)->transform.position.x-2.25f)<1e-5f);input.Start();input.Tick(1.0/30.0);assert(input.FrameCount()==1);input.Stop();
}
