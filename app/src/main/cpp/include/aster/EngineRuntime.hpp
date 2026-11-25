#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>

namespace aster {
struct Vec3 {
 float x{0},y{0},z{0};
 Vec3 operator+(Vec3 b)const{return{x+b.x,y+b.y,z+b.z};}
 Vec3 operator-(Vec3 b)const{return{x-b.x,y-b.y,z-b.z};}
 Vec3 operator*(float s)const{return{x*s,y*s,z*s};}
 static float Dot(Vec3 a,Vec3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}
 static Vec3 Cross(Vec3 a,Vec3 b){return{a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x};}
 Vec3 Normalized()const{float n=std::sqrt(x*x+y*y+z*z);return n>1e-6f?Vec3{x/n,y/n,z/n}:Vec3{};}
};
struct Mat4 {
 float m[16]{};
 static Mat4 Identity();
 static Mat4 Perspective(float fov,float aspect,float nearZ,float farZ);
 static Mat4 LookAt(Vec3 eye,Vec3 center,Vec3 up);
 static Mat4 Translation(Vec3 p);
 static Mat4 Scale(Vec3 s);
 static Mat4 RotationY(float r);
};
Mat4 operator*(const Mat4&a,const Mat4&b);
struct Transform { Vec3 position{}; Vec3 rotation{}; Vec3 scale{1,1,1}; };
using EntityId=std::uint32_t;
struct Entity { EntityId id{}; Transform transform{}; bool visible{true}; };
class Scene {
public:
 EntityId AddCube(Vec3 position);
 std::vector<Entity>& Entities(){return entities_;}
 const std::vector<Entity>& Entities()const{return entities_;}
private: std::vector<Entity> entities_; EntityId nextId_{1};
};
struct AssetRecord { std::uint32_t id{}; std::string name,type; };
class AssetRegistry {
public:
 std::uint32_t Register(std::string name,std::string type);
 const AssetRecord* Find(const std::string& name)const;
private: std::uint32_t next_{1}; std::unordered_map<std::string,AssetRecord> records_;
};
class PhysicsWorld {
public: void Step(Scene& scene,float dt);
private: Vec3 gravity_{0,-9.81f,0};
};
class Engine {
public:
 void Start(); void Stop(); void Tick(double delta);
 bool IsRunning()const{return running_;}
 std::uint64_t FrameCount()const{return frames_;}
 Scene& GetScene(){return scene_;}
 AssetRegistry& Assets(){return assets_;}
private:
 void FixedUpdate(float dt);
 bool running_{false}; double accumulator_{0}; double fixedStep_{1.0/60.0};
 std::uint64_t frames_{0}; Scene scene_; PhysicsWorld physics_; AssetRegistry assets_;
};
}
