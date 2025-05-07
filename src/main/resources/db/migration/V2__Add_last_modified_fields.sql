-- Son değişiklik bilgilerini izlemek için yeni sütunlar ekle
ALTER TABLE user_settings
    ADD COLUMN last_modified_by BIGINT,
ADD COLUMN last_modified_at TIMESTAMP;

-- Yeni eklenen last_modified_by sütunu için foreign key kısıtlaması ekle
ALTER TABLE user_settings
    ADD CONSTRAINT fk_user_settings_last_modified_by
        FOREIGN KEY (last_modified_by) REFERENCES users (id);

-- Mevcut kayıtlar için, son değişikliği yapan kullanıcıyı ayarla (user_id ile aynı olacak şekilde)
UPDATE user_settings
SET last_modified_by = user_id,
    last_modified_at = CURRENT_TIMESTAMP; 