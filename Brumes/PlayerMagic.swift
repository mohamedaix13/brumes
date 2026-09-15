import UIKit
import SceneKit

// MARK: - Spell Protocol
protocol Spell {
    var name: String { get }
    var cost: Float { get }
    var cooldown: Float { get }
    var color: UIColor { get }
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void)
}

// MARK: - Fire Spell
struct FireSpell: Spell {
    let name = "FEU"
    let cost: Float = 14
    let cooldown: Float = 0.55
    let color: UIColor = .systemOrange
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        guard let target = target else {
            completion(0)
            return
        }
        
        let destination = target.node.position
        let projectile = createProjectile(at: SCNVector3(origin.x, 1.4, origin.z), color: color)
        scene.rootNode.addChildNode(projectile)
        
        let trail = createParticles(color: color, count: 55, life: 0.3)
        projectile.addParticleSystem(trail)
        
        let duration = TimeInterval(max(0.12, distance(origin, destination) / 28))
        
        projectile.runAction(.sequence([
            .move(to: SCNVector3(destination.x, 1.1, destination.z), duration: duration),
            .run { [weak projectile] _ in
                guard let projectile = projectile else { return }
                burst(at: destination, color: color, radius: 1.5, in: scene)
                if let e = target, e.health > 0, distance(e.node.position, destination) < 3 {
                    hit(e, damage: 42, freeze: 0)
                }
                projectile.removeFromParentNode()
                completion(duration)
            }
        ]))
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Frost Spell
struct FrostSpell: Spell {
    let name = "GIVRE"
    let cost: Float = 25
    let cooldown: Float = 3.5
    let color: UIColor = .systemCyan
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        guard let target = target else {
            completion(0)
            return
        }
        
        let destination = target.node.position
        let projectile = createProjectile(at: SCNVector3(origin.x, 1.4, origin.z), color: color)
        scene.rootNode.addChildNode(projectile)
        
        let trail = createParticles(color: color, count: 55, life: 0.3)
        projectile.addParticleSystem(trail)
        
        let duration = TimeInterval(max(0.12, distance(origin, destination) / 28))
        
        projectile.runAction(.sequence([
            .move(to: SCNVector3(destination.x, 1.1, destination.z), duration: duration),
            .run { [weak projectile] _ in
                guard let projectile = projectile else { return }
                burst(at: destination, color: color, radius: 3, in: scene)
                if let e = target, e.health > 0, distance(e.node.position, destination) < 3 {
                    hit(e, damage: 25, freeze: 3)
                }
                projectile.removeFromParentNode()
                completion(duration)
            }
        ]))
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Wave Spell
struct WaveSpell: Spell {
    let name = "ONDE"
    let cost: Float = 38
    let cooldown: Float = 5.0
    let color: UIColor = .systemPurple
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        burst(at: origin, color: color, radius: 8, in: scene)
        for e in enemies where e.health > 0 && distance(origin, e.node.position) < 8 {
            hit(e, damage: 65, freeze: 0.8)
        }
        completion(0.5)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Mist Spell (NOUVEAU)
struct MistSpell: Spell {
    let name = "BRUME"
    let cost: Float = 20
    let cooldown: Float = 4.0
    let color: UIColor = .systemGray
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        burst(at: origin, color: color, radius: 6, in: scene)
        for e in enemies where e.health > 0 && distance(origin, e.node.position) < 6 {
            hit(e, damage: 15, freeze: 5.0)
        }
        completion(0.8)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Lightning Spell (NOUVEAU)
struct LightningSpell: Spell {
    let name = "FOUDRE"
    let cost: Float = 30
    let cooldown: Float = 4.5
    let color: UIColor = .systemYellow
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        let facing = player.eulerAngles.y
        let endX = origin.x + sin(facing) * 20
        let endZ = origin.z + cos(facing) * 20
        let endPosition = SCNVector3(endX, 0, endZ)
        
        createLightning(from: origin, to: endPosition, in: scene)
        
        for e in enemies where e.health > 0 {
            if pointOnLine(origin, endPosition, e.node.position, tolerance: 2.0) {
                hit(e, damage: 50, freeze: 0)
            }
        }
        completion(0.6)
    }
    
    private func pointOnLine(_ a: SCNVector3, _ b: SCNVector3, _ p: SCNVector3, tolerance: Float) -> Bool {
        let cross = (b.x - a.x) * (p.z - a.z) - (b.z - a.z) * (p.x - a.x)
        let len = hypot(b.x - a.x, b.z - a.z)
        return abs(cross) / len < tolerance
    }
    
    private func createLightning(from: SCNVector3, to: SCNVector3, in scene: SCNScene) {
        let lightning = SCNNode()
        lightning.position = from
        scene.rootNode.addChildNode(lightning)
        
        let geometry = SCNGeometry.lineGeometry(from: from, to: to, radius: 0.1)
        geometry.materials = [createMaterial(color: .systemYellow, glow: true)]
        lightning.geometry = geometry
        
        lightning.runAction(.sequence([
            .wait(duration: 0.5),
            .removeFromParentNode()
        ]))
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Light Spell (NOUVEAU - Soin)
struct LightSpell: Spell {
    let name = "LUMIÈRE"
    let cost: Float = 15
    let cooldown: Float = 3.0
    let color: UIColor = .systemYellow
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        burst(at: origin, color: color, radius: 2, in: scene)
        completion(0.4)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}

// MARK: - Player Magic Manager
class PlayerMagic {
    private var spells: [Spell] = []
    private var cooldowns: [Float] = []
    
    init() {
        spells = [
            FireSpell(),
            FrostSpell(),
            WaveSpell(),
            MistSpell(),
            LightningSpell(),
            LightSpell()
        ]
        cooldowns = Array(repeating: 0, count: spells.count)
    }
    
    func getSpell(index: Int) -> Spell? {
        return spells.indices.contains(index) ? spells[index] : nil
    }
    
    func castSpell(index: Int, from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, mana: inout Float, completion: @escaping () -> Void) -> Bool {
        guard spells.indices.contains(index) else {
            completion()
            return false
        }
        
        let spell = spells[index]
        
        if cooldowns[index] > 0 || mana < spell.cost {
            completion()
            return false
        }
        
        mana -= spell.cost
        cooldowns[index] = spell.cooldown
        
        spell.cast(from: origin, in: scene, target: target, player: player, enemies: enemies, shrines: shrines, active: active) { _ in
            completion()
        }
        
        return true
    }
    
    func update(dt: Float) {
        for i in 0..<cooldowns.count {
            cooldowns[i] = max(0, cooldowns[i] - dt)
        }
    }
    
    func getCooldown(index: Int) -> Float {
        return cooldowns.indices.contains(index) ? cooldowns[index] : 0
    }
    
    func getCost(index: Int) -> Float {
        return spells.indices.contains(index) ? spells[index].cost : 0
    }
    
    func getSpellNames() -> [String] {
        return spells.map { $0.name }
    }
    
    func getSpellColors() -> [UIColor] {
        return spells.map { $0.color }
    }
}

// MARK: - Helper Functions
func createProjectile(at position: SCNVector3, color: UIColor) -> SCNNode {
    let projectile = SCNNode(geometry: SCNSphere(radius: 0.23))
    projectile.geometry?.materials = [createMaterial(color: color, glow: true)]
    projectile.position = position
    return projectile
}

func createMaterial(color: UIColor, glow: Bool = false) -> SCNMaterial {
    let m = SCNMaterial()
    m.diffuse.contents = color
    m.roughness.contents = 0.85
    if glow {
        m.emission.contents = color
    }
    return m
}

func createParticles(color: UIColor, count: CGFloat, life: CGFloat) -> SCNParticleSystem {
    let p = SCNParticleSystem()
    p.birthRate = count
    p.particleLifeSpan = life
    p.particleSize = 0.1
    p.particleColor = color
    p.particleVelocity = 1.5
    p.spreadingAngle = 180
    p.blendMode = .additive
    p.isLightingEnabled = false
    return p
}

func burst(at position: SCNVector3, color: UIColor, radius: CGFloat, in scene: SCNScene) {
    let n = SCNNode()
    n.position = SCNVector3(position.x, 0.9, position.z)
    scene.rootNode.addChildNode(n)
    
    let p = createParticles(color: color, count: 230, life: 0.45)
    p.emissionDuration = 0.12
    p.loops = false
    p.particleVelocity = radius * 2
    n.addParticleSystem(p)
    
    let ring = SCNNode(geometry: SCNTorus(ringRadius: 0.35, pipeRadius: 0.035))
    ring.geometry?.materials = [createMaterial(color: color, glow: true)]
    n.addChildNode(ring)
    ring.runAction(.group([
        .scale(to: radius * 2, duration: 0.5),
        .fadeOut(duration: 0.5)
    ]))
    
    n.runAction(.sequence([
        .wait(duration: 1),
        .removeFromParentNode()
    ]))
}

func hit(_ e: Enemy, damage: Float, freeze: Float) {
    e.health -= damage
    e.frozen = max(e.frozen, freeze)
}

class Enemy {
    let node: SCNNode
    var health: Float = 100
    var frozen: Float = 0
    let home: SCNVector3
    
    init(_ node: SCNNode) {
        self.node = node
        home = node.position
    }
}