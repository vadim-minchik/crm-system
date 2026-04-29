package com.studio.crm_system.config;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.bootstrap.default-super-admin.enabled:true}")
    private boolean defaultSuperAdminEnabled;

    @Value("${app.bootstrap.default-super-admin.name:Admin}")
    private String defaultAdminName;

    @Value("${app.bootstrap.default-super-admin.surname:Admin}")
    private String defaultAdminSurname;

    @Value("${app.bootstrap.default-super-admin.patronymic:Admin}")
    private String defaultAdminPatronymic;

    @Value("${app.bootstrap.default-super-admin.login:}")
    private String defaultAdminLogin;

    @Value("${app.bootstrap.default-super-admin.password:}")
    private String defaultAdminPassword;

    @Value("${app.bootstrap.default-super-admin.email:admin@example.com}")
    private String defaultAdminEmail;

    @Value("${app.bootstrap.default-super-admin.phone:+375000000000}")
    private String defaultAdminPhone;

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

        boolean superAdminExists = userRepository.existsByRoleAndIsDeletedFalse(Role.SUPER_ADMIN);

        if (!superAdminExists && defaultSuperAdminEnabled) {
            if (defaultAdminLogin == null || defaultAdminLogin.isBlank()
                    || defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
                System.out.println("[DataInitializer] SUPER_ADMIN не создан: не заданы логин/пароль в app.bootstrap.default-super-admin.*");
            } else {
            User superAdmin = new User();
            superAdmin.setName(defaultAdminName);
            superAdmin.setSurname(defaultAdminSurname);
            superAdmin.setPatronymic(defaultAdminPatronymic);
            superAdmin.setLogin(defaultAdminLogin);
            superAdmin.setEmail(defaultAdminEmail);
            superAdmin.setPhoneNumber(defaultAdminPhone);
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setIsDeleted(false);
            superAdmin.setPoint(defaultPoint);

            superAdmin.setPassword(passwordEncoder.encode(defaultAdminPassword));

            userRepository.save(superAdmin);

            System.out.println("=================================================");
            System.out.println("  SUPER ADMIN создан автоматически:");
            System.out.println("  Логин:  " + defaultAdminLogin);
            System.out.println("  Пароль: задан через конфигурацию");
            System.out.println("  Рекомендуется сменить пароль после первого входа.");
            System.out.println("=================================================");
            }
        } else {
            System.out.println("[DataInitializer] SUPER_ADMIN уже существует или bootstrap отключен — пропускаем.");
        }

        for (User u : userRepository.findByIsDeletedFalseAndPointIsNull()) {
            u.setPoint(defaultPoint);
            userRepository.save(u);
            System.out.println("[DataInitializer] Назначена точка по умолчанию пользователю id=" + u.getId());
        }

        for (var e : equipmentRepository.findByIsDeletedFalseAndPointIsNull()) {
            e.setPoint(defaultPoint);
            equipmentRepository.save(e);
            System.out.println("[DataInitializer] Назначена точка по умолчанию экземпляру id=" + e.getId());
        }
    }
}
