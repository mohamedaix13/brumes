import UIKit
import SceneKit

// MARK: - Lightning Spell (Foudre)
struct LightningSpell: Spell {
    let name = "FOUDRE"
    let cost: Float = 30
    let cooldown: Float = 4.5
    let color: UIColor = .systemYellow
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        // Foudre : attaque en ligne droite devant le joueur
        let facing = player.eulerAngles.y
        let endX = origin.x + sin(facing) * 20
        let endZ = origin.z + cos(facing) * 20
        let endPosition = SCNVector3(endX, 0, endZ)
        
        // Effet visuel : ligne de foudre
        createLightning(from: origin, to: endPosition, in: scene)
        
        // Dégâts à tous les ennemis sur la ligne
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
        return len > 0 ? abs(cross) / len < tolerance : false
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