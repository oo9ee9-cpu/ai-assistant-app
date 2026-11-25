#pragma once
namespace aster {
class IRenderer {
public:
 virtual ~IRenderer()=default;
 virtual void Initialize()=0;
 virtual void Resize(int width,int height)=0;
 virtual void RenderFrame()=0;
 virtual void Shutdown()=0;
};
}
