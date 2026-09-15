import UIKit
import SceneKit

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