import json
import os
import requests
from bs4 import BeautifulSoup

# SimulationCraft URL
SIM_URL = "https://www.simulationcraft.org/reports/TWW2_Raid.html"

# Profil bilgileri
PROFILES = [
    # Death Knight
    {"class_name": "Death_Knight", "spec_name": "Blood", "hero_talent": "Deathbringer", "player_id": "player2"},
    {"class_name": "Death_Knight", "spec_name": "Blood", "hero_talent": "San'layn", "player_id": "player1"},
    {"class_name": "Death_Knight", "spec_name": "Frost", "hero_talent": "Deathbringer", "player_id": "player4"},
    {"class_name": "Death_Knight", "spec_name": "Frost", "hero_talent": "Rider", "player_id": "player3"},
    {"class_name": "Death_Knight", "spec_name": "Unholy", "hero_talent": "Rider", "player_id": "player6"},
    {"class_name": "Death_Knight", "spec_name": "Unholy", "hero_talent": "San'layn", "player_id": "player5"},

    # Demon Hunter
    {"class_name": "Demon_Hunter", "spec_name": "Havoc", "hero_talent": "", "player_id": "player7"},

    # Druid
    {"class_name": "Druid", "spec_name": "Balance", "hero_talent": "", "player_id": "player8"},
    {"class_name": "Druid", "spec_name": "Feral", "hero_talent": "", "player_id": "player9"},

    # Evoker
    {"class_name": "Evoker", "spec_name": "Devastation", "hero_talent": "Flameshaper", "player_id": "player10"},
    {"class_name": "Evoker", "spec_name": "Devastation", "hero_talent": "Scalecommander", "player_id": "player11"},

    # Hunter
    {"class_name": "Hunter", "spec_name": "Beast_Mastery", "hero_talent": "Pack_Leader", "player_id": "player12"},
    {"class_name": "Hunter", "spec_name": "Marksmanship", "hero_talent": "Dark_Ranger", "player_id": "player13"},
    {"class_name": "Hunter", "spec_name": "Survival", "hero_talent": "Pack_Leader", "player_id": "player14"},

    # Mage
    {"class_name": "Mage", "spec_name": "Arcane", "hero_talent": "Spellslinger", "player_id": "player15"},
    {"class_name": "Mage", "spec_name": "Arcane", "hero_talent": "Sunfury", "player_id": "player16"},
    {"class_name": "Mage", "spec_name": "Fire", "hero_talent": "Frostfire", "player_id": "player17"},
    {"class_name": "Mage", "spec_name": "Fire", "hero_talent": "Sunfury", "player_id": "player18"},
    {"class_name": "Mage", "spec_name": "Frost", "hero_talent": "Frostfire", "player_id": "player19"},
    {"class_name": "Mage", "spec_name": "Frost", "hero_talent": "Spellslinger", "player_id": "player20"},

    # Monk
    {"class_name": "Monk", "spec_name": "Brewmaster", "hero_talent": "", "player_id": "player21"},
    {"class_name": "Monk", "spec_name": "Windwalker", "hero_talent": "", "player_id": "player22"},
    {"class_name": "Monk", "spec_name": "Windwalker", "hero_talent": "Shadopan", "player_id": "player23"},

    # Paladin
    {"class_name": "Paladin", "spec_name": "Protection", "hero_talent": "Lightsmith", "player_id": "player24"},
    {"class_name": "Paladin", "spec_name": "Retribution", "hero_talent": "", "player_id": "player25"},
    {"class_name": "Paladin", "spec_name": "Retribution", "hero_talent": "Herald", "player_id": "player27"},
    {"class_name": "Paladin", "spec_name": "Retribution", "hero_talent": "Templar", "player_id": "player26"},

    # Priest
    {"class_name": "Priest", "spec_name": "Shadow", "hero_talent": "Archon", "player_id": "player29"},
    {"class_name": "Priest", "spec_name": "Shadow", "hero_talent": "Voidweaver", "player_id": "player28"},

    # Rogue
    {"class_name": "Rogue", "spec_name": "Assassination", "hero_talent": "", "player_id": "player30"},
    {"class_name": "Rogue", "spec_name": "Outlaw", "hero_talent": "", "player_id": "player31"},
    {"class_name": "Rogue", "spec_name": "Subtlety", "hero_talent": "", "player_id": "player32"},

    # Shaman
    {"class_name": "Shaman", "spec_name": "Elemental", "hero_talent": "Farseer", "player_id": "player33"},
    {"class_name": "Shaman", "spec_name": "Enhancement", "hero_talent": "", "player_id": "player34"},
    {"class_name": "Shaman", "spec_name": "Enhancement", "hero_talent": "Stormbringer", "player_id": "player35"},

    # Warlock
    {"class_name": "Warlock", "spec_name": "Affliction", "hero_talent": "Hellcaller", "player_id": "player36"},
    {"class_name": "Warlock", "spec_name": "Demonology", "hero_talent": "Diabolist", "player_id": "player37"},
    {"class_name": "Warlock", "spec_name": "Destruction", "hero_talent": "Diabolist", "player_id": "player38"},

    # Warrior
    {"class_name": "Warrior", "spec_name": "Arms", "hero_talent": "", "player_id": "player39"},
    {"class_name": "Warrior", "spec_name": "Fury", "hero_talent": "", "player_id": "player40"},
    {"class_name": "Warrior", "spec_name": "Protection", "hero_talent": "Colossus", "player_id": "player41"},
    {"class_name": "Warrior", "spec_name": "Protection", "hero_talent": "Thane", "player_id": "player42"}
]

def extract_bis_gear(soup, profile):
    """HTML'den BIS ekipman bilgilerini çıkarır"""
    class_name = profile["class_name"]
    spec_name = profile["spec_name"]
    hero_talent = profile["hero_talent"]
    player_id = profile["player_id"]
    
    print(f"Extracting BIS gear for {class_name} {spec_name} {hero_talent}...")
    
    # Player div'ini bul
    player_div = soup.find(id=player_id)
    if not player_div:
        print(f"Could not find player div with ID: {player_id}")
        return {}
    
    # Gear bölümünü bul
    gear_sections = player_div.select("div.player-section.gear")
    
    if not gear_sections:
        print(f"Could not find gear section for {class_name} {spec_name} {hero_talent}")
        return {}
    
    gear_section = gear_sections[0]
    
    # Gear tablosunu bul
    gear_table = gear_section.select_one("div.toggle-content table")
    
    if not gear_table:
        print(f"Could not find gear table for {class_name} {spec_name} {hero_talent}")
        return {}
    
    # BIS ekipmanları çıkar
    bis_items = {}
    
    # Tüm satırları al
    rows = gear_table.select("tbody > tr")
    
    # Satırları ikişer ikişer işle (slot satırı ve stat satırı)
    i = 0
    while i < len(rows) - 1:
        # Slot satırı
        slot_row = rows[i]
        
        # Slot adını al
        slot_cells = slot_row.select("th")
        if len(slot_cells) < 2:
            i += 2
            continue
        
        slot_name = slot_cells[1].text.strip()
        
        # İtem hücresini al
        item_cells = slot_row.select("td")
        if not item_cells:
            i += 2
            continue
        
        item_cell = item_cells[0]
        
        # İtem adını ve linkini al
        item_link = item_cell.select_one("a")
        
        item_name = ""
        item_url = ""
        
        if item_link:
            item_name = item_link.text.strip()
            if item_link.has_attr("href"):
                item_url = item_link["href"]
        else:
            item_name = item_cell.text.strip()
        
        # Stat satırı (bir sonraki satır)
        stats = ""
        if i + 1 < len(rows):
            stat_row = rows[i + 1]
            stat_cells = stat_row.select("td")
            if stat_cells:
                stats = stat_cells[0].text.strip()
        
        # Kaynak bilgisini al
        source = ""
        if len(slot_cells) > 0:
            source = slot_cells[0].text.strip()
        
        if slot_name and item_name:
            bis_items[slot_name] = {
                "name": item_name,
                "url": item_url,
                "source": source,
                "stats": stats
            }
        
        # Sonraki slot satırına geç (2 satır atla)
        i += 2
    
    # Bulunan BIS itemleri konsola logla
    if bis_items:
        print(f"\nFound {len(bis_items)} BIS items for {class_name} {spec_name} {hero_talent}:")
        for slot, item_info in bis_items.items():
            print(f"  {slot}: {item_info['name']} (Source: {item_info['source']})")
        print("")
    else:
        print(f"No BIS items found for {class_name} {spec_name} {hero_talent}\n")
    
    return bis_items

def main():
    """Ana fonksiyon"""
    try:
        # SimulationCraft sayfasını bir kez çek
        print(f"Fetching SimulationCraft page from {SIM_URL}...")
        response = requests.get(SIM_URL, timeout=60)  # Daha uzun timeout
        response.raise_for_status()
        
        # HTML'i parse et
        print("Parsing HTML...")
        soup = BeautifulSoup(response.text, "html.parser")
        
        # Tüm BIS verilerini içeren ana sözlük
        all_bis_data = {}
        
        # Toplam profil sayısını ve işlenen profil sayısını takip et
        total_profiles = len(PROFILES)
        processed_profiles = 0
        successful_profiles = 0
        
        print(f"\nProcessing {total_profiles} profiles...\n")
        
        # Tüm profilleri işle
        for profile in PROFILES:
            try:
                bis_items = extract_bis_gear(soup, profile)
                processed_profiles += 1
                
                if bis_items:
                    # Profil anahtarını oluştur
                    profile_key = f"{profile['class_name']}_{profile['spec_name']}"
                    if profile['hero_talent']:
                        profile_key += f"_{profile['hero_talent']}"
                    
                    # Verileri ana sözlüğe ekle
                    all_bis_data[profile_key] = bis_items
                    
                    successful_profiles += 1
                    print(f"Successfully processed profile: {profile_key}")
                
                # İlerleme durumunu göster
                print(f"Progress: {processed_profiles}/{total_profiles} profiles processed ({successful_profiles} successful)")
                print("-" * 80)
            except Exception as e:
                processed_profiles += 1
                print(f"Error processing profile {profile}: {e}")
                print("-" * 80)
        
        # Tüm verileri tek bir JSON dosyasına kaydet
        output_file = "bis_data.json"
        with open(output_file, "w") as f:
            json.dump(all_bis_data, f, indent=2)
        
        print(f"\nAll BIS data saved to {output_file}")
        print(f"Successfully processed {successful_profiles}/{total_profiles} profiles.")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main() 