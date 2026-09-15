import UIKit
import SceneKit

// MARK: - Frost Spell (Givre)
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