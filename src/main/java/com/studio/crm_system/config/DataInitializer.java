package com.studio.crm_system.config;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (pointRepository.count() == 0) {
            Point main = new Point();
            main.setName("Основная");
            main.setAddress(null);
            main.setIsDeleted(false);
            pointRepository.save(main);
            System.out.println("[DataInitializer] Создана точка по умолчанию: Основная");
        }

        Point defaultPoint = pointRepository.findByIsDeletedFalseOrderByNameAsc().get(0);

        boolean superAdminExists = userRepository.findAll()
                .stream()
                .anyMatch(u -> u.getRole() == Role.SUPER_ADMIN && !u.getIsDeleted());

        if (!superAdminExists) {
            User superAdmin = new User();
            superAdmin.setName("Вадим");
            superAdmin.setSurname("Минчик");
            superAdmin.setPatronymic("Андреевич");
            superAdmin.setLogin("ztryuiorust");
            superAdmin.setEmail("vadim.minchik90@gmail.com");
            superAdmin.setPhoneNumber("+375291573109");
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setIsDeleted(false);
            superAdmin.setPoint(defaultPoint);

            superAdmin.setPassword(passwordEncoder.encode("!22233loim"));

            userRepository.save(superAdmin);

            System.out.println("=================================================");
            System.out.println("  SUPER ADMIN создан автоматически:");
            System.out.println("  Логин:  ztryuiorust");
            System.out.println("  Пароль: !22233loim");
            System.out.println("  Смени пароль после первого входа!");
            System.out.println("=================================================");
        } else {
            System.out.println("[DataInitializer] SUPER_ADMIN уже существует — пропускаем.");
        }

        for (User u : userRepository.findAll()) {
            if (!Boolean.TRUE.equals(u.getIsDeleted()) && u.getPoint() == null) {
                u.setPoint(defaultPoint);
                userRepository.save(u);
                System.out.println("[DataInitializer] Назначена точка по умолчанию пользователю id=" + u.getId());
            }
        }

        for (var e : equipmentRepository.findAll()) {
            if (!Boolean.TRUE.equals(e.getIsDeleted()) && e.getPoint() == null) {
                e.setPoint(defaultPoint);
                equipmentRepository.save(e);
                System.out.println("[DataInitializer] Назначена точка по умолчанию экземпляру id=" + e.getId());
            }
        }
    }
}
