package com.studio.crm_system.config;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Инициализатор данных — запускается при старте приложения.
 * Если в БД нет ни одного SUPER_ADMIN, создаёт его автоматически.
 *
 * Логин:  superadmin
 * Пароль: admin123
 *
 * После первого входа смени пароль в интерфейсе.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Одна точка по умолчанию, если точек ещё нет (для расширения на несколько точек)
        if (pointRepository.count() == 0) {
            Point main = new Point();
            main.setName("Основная");
            main.setAddress(null);
            main.setIsDeleted(false);
            pointRepository.save(main);
            System.out.println("[DataInitializer] Создана точка по умолчанию: Основная");
        }

        // Проверяем: есть ли уже хоть один SUPER_ADMIN?
        boolean superAdminExists = userRepository.findAll()
                .stream()
                .anyMatch(u -> u.getRole() == Role.SUPER_ADMIN && !u.getIsDeleted());

        if (!superAdminExists) {
            User superAdmin = new User();
            superAdmin.setName("Супер");
            superAdmin.setSurname("Администратор");
            superAdmin.setPatronymic("-");
            superAdmin.setLogin("superadmin");
            superAdmin.setEmail("superadmin@studio.crm");
            superAdmin.setPhoneNumber("+70000000000");
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setIsDeleted(false);

            // Пароль хешируется автоматически через BCrypt
            superAdmin.setPassword(passwordEncoder.encode("admin123"));

            userRepository.save(superAdmin);

            System.out.println("=================================================");
            System.out.println("  SUPER ADMIN создан автоматически:");
            System.out.println("  Логин:  superadmin");
            System.out.println("  Пароль: admin123");
            System.out.println("  Смени пароль после первого входа!");
            System.out.println("=================================================");
        } else {
            System.out.println("[DataInitializer] SUPER_ADMIN уже существует — пропускаем.");
        }
    }
}
