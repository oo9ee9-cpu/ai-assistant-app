#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>
namespace aster {
struct Vec3{
 float x{0},y{0},z{0};
 Vec3 operator+(Vec3 b)const{return{x+b.x,y+b.y,z+b.z};}
 Vec3 operator-(Vec3 b)const{return{x-b.x,y-b.y,z-b.z};}
 Vec3 operator*(float s)const{return{x*s,y*s,z*s};}
 Vec3& operator+=(Vec3 b){x+=b.x;y+=b.y;z+=b.z;return *this;}
 static float Dot(Vec3 a,Vec3 b){return a.x*b.x+a.y*b.y+a.z*b.z;}
 static Vec3 Cross(Vec3 a,Vec3 b){return{a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x};}
 Vec3 Normalized()const{float n=std::sqrt(x*x+y*y+z*z);return n>1e-6f?Vec3{x/n,y/n,z/n}:Vec3{};}
};
struct Mat4{
 float m[16]{};
 static Mat4 Identity();static Mat4 Perspective(float,float,float,float);static Mat4 LookAt(Vec3,Vec3,Vec3);
 static Mat4 Translation(Vec3);static Mat4 Scale(Vec3);static Mat4 RotationX(float);static Mat4 RotationY(float);static Mat4 RotationZ(float);
};
Mat4 operator*(const Mat4&,const Mat4&);
struct Transform{Vec3 position{};Vec3 rotation{};Vec3 scale{1,1,1};};
using EntityId=std::uint32_t;
enum class EntityKind:std::uint8_t{Cube,Sphere,Light,Camera};
struct RigidBody{
 Vec3 velocity{};float mass{1.0f};float restitution{0.05f};float friction{0.65f};
 bool isStatic{false};bool useGravity{true};bool grounded{false};
};
struct Entity{EntityId id{};EntityKind kind{EntityKind::Cube};Transform transform{};RigidBody body{};bool visible{true};};
class Scene{
public:EntityId Add(EntityKind,Vec3);void Clear(){entities_.clear();nextId_=1;}std::vector<Entity>& Entities(){return entities_;}const std::vector<Entity>& Entities()const{return entities_;}
 Entity* Find(EntityId);const Entity* Find(EntityId)const;
private:std::vector<Entity> entities_;EntityId nextId_{1};
};
struct AssetRecord{std::uint32_t id{};std::string name,type;};
class AssetRegistry{public:std::uint32_t Register(std::string,std::string);const AssetRecord* Find(const std::string&)const;private:std::uint32_t next_{1};std::unordered_map<std::string,AssetRecord> records_;};
class PhysicsWorld{
public:
 void Step(Scene&,float);
private:
 static bool IsBody(const Entity&);
 static Vec3 HalfExtents(const Entity&);
 static void ResolveGround(Entity&);
 static void ResolvePair(Entity&,Entity&);
};
class Engine{
public:void Start();void Stop();void Tick(double);bool IsRunning()const{return running_;}std::uint64_t FrameCount()const{return frames_;}
 Scene& GetScene(){return scene_;}AssetRegistry& Assets(){return assets_;}
 void Select(EntityId id){selected_=id;}EntityId Selected()const{return selected_;}
 void SetPosition(EntityId,Vec3);void SetScale(EntityId,Vec3);void SetRotation(EntityId,Vec3);void SetStatic(EntityId,bool); void SetBodyProperties(EntityId,float,float,float,bool);
 void SetVelocity(EntityId,Vec3);Vec3 Velocity(EntityId)const;
 void DragSelected(float dx,float dy);void DragSelectedAxis(int axis,float amount);
 void Orbit(float dx,float dy);void Pan(float dx,float dy);void Zoom(float factor);
 Vec3 CameraPosition()const;Vec3 CameraTarget()const{return cameraTarget_;}
 float CameraYaw()const{return cameraYaw_;}float CameraPitch()const{return cameraPitch_;}float CameraDistance()const{return cameraDistance_;}
private:
 struct StartState{Transform transform;Vec3 velocity;};
 void FixedUpdate(float);
 std::unordered_map<EntityId,StartState> startState_;
 bool running_{false};double accumulator_{0},fixedStep_{1.0/60.0};std::uint64_t frames_{0};
 Scene scene_;PhysicsWorld physics_;AssetRegistry assets_;EntityId selected_{2};
 Vec3 cameraTarget_{0,0,0};float cameraYaw_{0.62f},cameraPitch_{0.43f},cameraDistance_{8.0f};
};
}
