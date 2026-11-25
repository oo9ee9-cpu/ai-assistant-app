#include "aster/EngineRuntime.hpp"
#include <cassert>
int main(){aster::Vec3 a{1,2,3},b{2,1,0};auto c=a+b;assert(c.x==3&&c.y==3&&c.z==3);aster::Engine e;e.Start();assert(e.IsRunning());assert(e.GetScene().Entities().size()==2);e.Tick(1.0/30.0);assert(e.FrameCount()==1);e.GetScene().AddCube({0,0,0});assert(e.GetScene().Entities().size()==3);e.Stop();assert(!e.IsRunning());}
