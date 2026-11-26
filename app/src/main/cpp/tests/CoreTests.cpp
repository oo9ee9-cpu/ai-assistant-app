#include "aster/EngineRuntime.hpp"
#include <cassert>
int main(){aster::Engine e;e.Start();assert(e.IsRunning());assert(e.GetScene().Entities().size()==2);e.Tick(1.0/30.0);assert(e.FrameCount()==1);auto id=e.GetScene().Add(aster::EntityKind::Cube,{0,1,0});e.Select(id);e.SetPosition(id,{1,2,3});e.SetScale(id,{2,2,2});e.Orbit(10,4);e.Pan(2,2);e.Zoom(.9f);assert(e.GetScene().Find(id)->transform.position.x==1);e.Stop();assert(!e.IsRunning());}
