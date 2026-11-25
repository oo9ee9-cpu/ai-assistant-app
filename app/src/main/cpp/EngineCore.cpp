#include "EngineCore.hpp"
namespace aster {
void EngineCore::Start() noexcept { running_ = true; frameCount_ = 0; }
void EngineCore::Tick(double deltaSeconds) noexcept {
    if (running_ && deltaSeconds >= 0.0) ++frameCount_;
}
void EngineCore::Stop() noexcept { running_ = false; }
bool EngineCore::IsRunning() const noexcept { return running_; }
std::uint64_t EngineCore::FrameCount() const noexcept { return frameCount_; }
}
