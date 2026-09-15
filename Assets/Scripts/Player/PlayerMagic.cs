using UnityEngine;
using System.Collections.Generic;

public class PlayerMagic : MonoBehaviour
{
    public List<Spell> spells = new List<Spell>();
    public int mana = 100;
    public int maxMana = 100;
    public float manaRegenRate = 5f;
    public Transform spellSpawnPoint;
    
    private float manaRegenTimer = 0f;
    
    void Update()
    {
        manaRegenTimer += Time.deltaTime;
        if (manaRegenTimer >= 1f)
        {
            mana = Mathf.Min(mana + Mathf.FloorToInt(manaRegenRate), maxMana);
            manaRegenTimer = 0f;
        }
        
        if (Input.GetKeyDown(KeyCode.Space) && spells.Count > 0 && mana >= spells[0].manaCost)
        {
            CastSpell(0);
        }
    }
    
    public void CastSpell(int spellIndex)
    {
        if (spellIndex < 0 || spellIndex >= spells.Count) return;
        Spell spell = spells[spellIndex];
        if (mana < spell.manaCost) return;
        mana -= spell.manaCost;
        if (spell.prefab != null && spellSpawnPoint != null)
        {
            GameObject spellObj = Instantiate(spell.prefab, spellSpawnPoint.position, spellSpawnPoint.rotation);
        }
        Debug.Log("Sort lance: " + spell.spellName);
    }
}

[System.Serializable]
public class Spell
{
    public string spellName;
    public GameObject prefab;
    public int manaCost;
    public float cooldown;
    public float damage;
    public ElementType elementType;
}

public enum ElementType
{
    Fire, Ice, Lightning, Mist, Light, Shadow
}