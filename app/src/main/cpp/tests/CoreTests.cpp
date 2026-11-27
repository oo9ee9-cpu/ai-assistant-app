#include "aster/EngineRuntime.hpp"
#include <cassert>
#include <cmath>
int main(){
 aster::Engine e;
 e.GetScene().Add(aster::EntityKind::Cube,{0,0,0});
 e.GetScene().Add(aster::EntityKind::Cube,{2,0,0});
 e.Start();assert(e.IsRunning());assert(e.GetScene().Entities().size()==2);
 e.Tick(1.0/30.0);assert(e.FrameCount()==1);
 const auto id=e.GetScene().Add(aster::EntityKind::Cube,{0,1,0});e.Select(id);
 e.SetPosition(id,{1,2,3});e.SetScale(id,{2,2,2});
 e.DragSelectedAxis(1,1.25f);auto*p=e.GetScene().Find(id);
 assert(std::fabs(p->transform.position.x-2.25f)<1e-5f);
 assert(std::fabs(p->transform.position.y-2.0f)<1e-5f);
 assert(std::fabs(p->transform.position.z-3.0f)<1e-5f);
 e.DragSelectedAxis(2,-.5f);assert(std::fabs(p->transform.position.y-1.5f)<1e-5f);
 e.DragSelectedAxis(3,2.0f);assert(std::fabs(p->transform.position.z-5.0f)<1e-5f);
 e.Orbit(10,4);e.Pan(2,2);e.Zoom(.9f);
 e.Stop();assert(!e.IsRunning());
}