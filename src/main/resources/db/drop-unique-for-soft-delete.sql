-- Один раз выполнить после обновления кода (мягкое удаление).
-- Снимает уникальные ограничения с полей, чтобы удалённые записи не мешали
-- создавать новые с теми же значениями (телефон, паспорт, логин и т.д.).
-- Имена ограничений могут отличаться (зависит от Hibernate/БД).
-- Узнать имена: SELECT conname FROM pg_constraint WHERE conrelid = 'clients'::regclass AND contype = 'u';

-- Клиенты: телефон, паспорт, идентификационный номер
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_phone_number_key;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_passport_number_key;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_identification_number_key;

-- Пользователи: email, login
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_login_key;

-- Оборудование: серийный номер
ALTER TABLE equipment DROP CONSTRAINT IF EXISTS equipment_serial_number_key;
