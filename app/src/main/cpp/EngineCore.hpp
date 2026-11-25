#pragma once
#include <cstdint>
namespace aster {
class EngineCore final {
public:
    void Start() noexcept;
    void Tick(double deltaSeconds) noexcept;
    void Stop() noexcept;
    [[nodiscard]] bool IsRunning() const noexcept;
    [[nodiscard]] std::uint64_t FrameCount() const noexcept;
private:
    bool running_ = false;
    std::uint64_t frameCount_ = 0;
};
}
