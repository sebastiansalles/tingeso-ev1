-- Initial data: Appendix A (careers, study plans, subjects and prerequisites).
-- INSERT IGNORE + unique constraints make this script safe to run on every startup:
-- rows that already exist are skipped instead of failing.

-- ===== 10450 - Técnico Analista Programador (plan 2021.3) =====
INSERT IGNORE INTO careers (code, name, description, status) VALUES ('10450', 'Técnico Analista Programador', 'Carrera técnica de dos años orientada al análisis y desarrollo de software.', 'ACTIVE');
INSERT IGNORE INTO study_plans (career_id, identifier, status)
SELECT id, '2021.3', 'CURRENT' FROM careers WHERE code = '10450';

INSERT IGNORE INTO subjects (study_plan_id, code, name, semester, theory_hours, exercise_hours, lab_hours, sct)
SELECT sp.id, v.code, v.name, v.semester, v.t, v.e, v.l, v.sct
FROM study_plans sp JOIN careers c ON c.id = sp.career_id
                    JOIN (
    SELECT 'TAP101' AS code, 'Fundamentos de Programación' AS name, 1 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP102' AS code, 'Matemática Aplicada' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP103' AS code, 'Fundamentos de Computación' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP104' AS code, 'Bases de Datos I' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP105' AS code, 'Comunicación Efectiva' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP106' AS code, 'Inglés Técnico I' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP201' AS code, 'Programación Orientada a Objetos' AS name, 2 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP202' AS code, 'Estructuras de Datos' AS name, 2 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP203' AS code, 'Bases de Datos II' AS name, 2 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP204' AS code, 'Desarrollo Web I' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP205' AS code, 'Sistemas Operativos' AS name, 2 AS semester, 2 AS t, 0 AS e, 2 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP206' AS code, 'Inglés Técnico II' AS name, 2 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
    UNION ALL
    SELECT 'TAP301' AS code, 'Desarrollo Web II' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP302' AS code, 'Ingeniería de Software' AS name, 3 AS semester, 2 AS t, 2 AS e, 0 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP303' AS code, 'Desarrollo de Aplicaciones Móviles' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP304' AS code, 'Administración de Bases de Datos' AS name, 3 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP305' AS code, 'Redes y Comunicaciones' AS name, 3 AS semester, 2 AS t, 0 AS e, 2 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP306' AS code, 'Taller de Programación' AS name, 3 AS semester, 0 AS t, 0 AS e, 4 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP401' AS code, 'Arquitectura y Desarrollo de Software' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP402' AS code, 'Desarrollo de Aplicaciones Cloud' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP403' AS code, 'Seguridad de Aplicaciones' AS name, 4 AS semester, 2 AS t, 0 AS e, 2 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAP404' AS code, 'Calidad y Pruebas de Software' AS name, 4 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAP405' AS code, 'Proyecto de Desarrollo de Software' AS name, 4 AS semester, 0 AS t, 0 AS e, 6 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAP406' AS code, 'Emprendimiento y Gestión de Proyectos' AS name, 4 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
) v
WHERE c.code = '10450' AND sp.identifier = '2021.3';

INSERT IGNORE INTO prerequisites (subject_id, prerequisite_id)
SELECT s.id, p.id
FROM subjects s
         JOIN subjects p ON p.study_plan_id = s.study_plan_id
         JOIN study_plans sp ON sp.id = s.study_plan_id
         JOIN careers c ON c.id = sp.career_id
         JOIN (
    SELECT 'TAP201' AS subject_code, 'TAP101' AS prerequisite_code
    UNION ALL
    SELECT 'TAP202' AS subject_code, 'TAP101' AS prerequisite_code
    UNION ALL
    SELECT 'TAP203' AS subject_code, 'TAP104' AS prerequisite_code
    UNION ALL
    SELECT 'TAP204' AS subject_code, 'TAP101' AS prerequisite_code
    UNION ALL
    SELECT 'TAP205' AS subject_code, 'TAP103' AS prerequisite_code
    UNION ALL
    SELECT 'TAP206' AS subject_code, 'TAP106' AS prerequisite_code
    UNION ALL
    SELECT 'TAP301' AS subject_code, 'TAP201' AS prerequisite_code
    UNION ALL
    SELECT 'TAP301' AS subject_code, 'TAP204' AS prerequisite_code
    UNION ALL
    SELECT 'TAP302' AS subject_code, 'TAP201' AS prerequisite_code
    UNION ALL
    SELECT 'TAP303' AS subject_code, 'TAP201' AS prerequisite_code
    UNION ALL
    SELECT 'TAP304' AS subject_code, 'TAP203' AS prerequisite_code
    UNION ALL
    SELECT 'TAP305' AS subject_code, 'TAP205' AS prerequisite_code
    UNION ALL
    SELECT 'TAP306' AS subject_code, 'TAP201' AS prerequisite_code
    UNION ALL
    SELECT 'TAP306' AS subject_code, 'TAP202' AS prerequisite_code
    UNION ALL
    SELECT 'TAP401' AS subject_code, 'TAP301' AS prerequisite_code
    UNION ALL
    SELECT 'TAP401' AS subject_code, 'TAP302' AS prerequisite_code
    UNION ALL
    SELECT 'TAP402' AS subject_code, 'TAP301' AS prerequisite_code
    UNION ALL
    SELECT 'TAP403' AS subject_code, 'TAP301' AS prerequisite_code
    UNION ALL
    SELECT 'TAP403' AS subject_code, 'TAP305' AS prerequisite_code
    UNION ALL
    SELECT 'TAP404' AS subject_code, 'TAP302' AS prerequisite_code
    UNION ALL
    SELECT 'TAP405' AS subject_code, 'TAP301' AS prerequisite_code
    UNION ALL
    SELECT 'TAP405' AS subject_code, 'TAP302' AS prerequisite_code
    UNION ALL
    SELECT 'TAP405' AS subject_code, 'TAP306' AS prerequisite_code
    UNION ALL
    SELECT 'TAP406' AS subject_code, 'TAP302' AS prerequisite_code
) v ON v.subject_code = s.code AND v.prerequisite_code = p.code
WHERE c.code = '10450' AND sp.identifier = '2021.3';

-- ===== 10451 - Técnico en Análisis de Datos (plan 2022.3) =====
INSERT IGNORE INTO careers (code, name, description, status) VALUES ('10451', 'Técnico en Análisis de Datos', 'Carrera técnica de dos años orientada al análisis y procesamiento de datos.', 'ACTIVE');
INSERT IGNORE INTO study_plans (career_id, identifier, status)
SELECT id, '2022.3', 'CURRENT' FROM careers WHERE code = '10451';

INSERT IGNORE INTO subjects (study_plan_id, code, name, semester, theory_hours, exercise_hours, lab_hours, sct)
SELECT sp.id, v.code, v.name, v.semester, v.t, v.e, v.l, v.sct
FROM study_plans sp JOIN careers c ON c.id = sp.career_id
                    JOIN (
    SELECT 'TAD101' AS code, 'Fundamentos de Programación' AS name, 1 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD102' AS code, 'Matemática Aplicada' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD103' AS code, 'Introducción al Análisis de Datos' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD104' AS code, 'Bases de Datos I' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD105' AS code, 'Herramientas Computacionales' AS name, 1 AS semester, 0 AS t, 0 AS e, 4 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAD106' AS code, 'Comunicación Efectiva' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAD201' AS code, 'Programación para Análisis de Datos' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD202' AS code, 'Probabilidad y Estadística' AS name, 2 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD203' AS code, 'Bases de Datos II' AS name, 2 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD204' AS code, 'Preparación y Calidad de Datos' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD205' AS code, 'Visualización de Datos' AS name, 2 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD206' AS code, 'Inglés Técnico' AS name, 2 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
    UNION ALL
    SELECT 'TAD301' AS code, 'Estadística Aplicada' AS name, 3 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD302' AS code, 'Minería de Datos' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD303' AS code, 'Inteligencia de Negocios' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD304' AS code, 'Integración y Procesamiento de Datos' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD305' AS code, 'Visualización Avanzada de Datos' AS name, 3 AS semester, 2 AS t, 0 AS e, 2 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAD306' AS code, 'Ética y Protección de Datos' AS name, 3 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TAD401' AS code, 'Fundamentos de Machine Learning' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD402' AS code, 'Big Data y Procesamiento de Datos' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD403' AS code, 'Analítica de Negocios' AS name, 4 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD404' AS code, 'Desarrollo de Soluciones de Datos' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TAD405' AS code, 'Proyecto de Análisis de Datos' AS name, 4 AS semester, 0 AS t, 0 AS e, 6 AS l, 6 AS sct
    UNION ALL
    SELECT 'TAD406' AS code, 'Emprendimiento y Gestión de Proyectos' AS name, 4 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
) v
WHERE c.code = '10451' AND sp.identifier = '2022.3';

INSERT IGNORE INTO prerequisites (subject_id, prerequisite_id)
SELECT s.id, p.id
FROM subjects s
         JOIN subjects p ON p.study_plan_id = s.study_plan_id
         JOIN study_plans sp ON sp.id = s.study_plan_id
         JOIN careers c ON c.id = sp.career_id
         JOIN (
    SELECT 'TAD201' AS subject_code, 'TAD101' AS prerequisite_code
    UNION ALL
    SELECT 'TAD202' AS subject_code, 'TAD102' AS prerequisite_code
    UNION ALL
    SELECT 'TAD203' AS subject_code, 'TAD104' AS prerequisite_code
    UNION ALL
    SELECT 'TAD204' AS subject_code, 'TAD103' AS prerequisite_code
    UNION ALL
    SELECT 'TAD204' AS subject_code, 'TAD101' AS prerequisite_code
    UNION ALL
    SELECT 'TAD205' AS subject_code, 'TAD103' AS prerequisite_code
    UNION ALL
    SELECT 'TAD301' AS subject_code, 'TAD202' AS prerequisite_code
    UNION ALL
    SELECT 'TAD302' AS subject_code, 'TAD201' AS prerequisite_code
    UNION ALL
    SELECT 'TAD302' AS subject_code, 'TAD202' AS prerequisite_code
    UNION ALL
    SELECT 'TAD303' AS subject_code, 'TAD203' AS prerequisite_code
    UNION ALL
    SELECT 'TAD303' AS subject_code, 'TAD205' AS prerequisite_code
    UNION ALL
    SELECT 'TAD304' AS subject_code, 'TAD201' AS prerequisite_code
    UNION ALL
    SELECT 'TAD304' AS subject_code, 'TAD203' AS prerequisite_code
    UNION ALL
    SELECT 'TAD305' AS subject_code, 'TAD205' AS prerequisite_code
    UNION ALL
    SELECT 'TAD306' AS subject_code, 'TAD103' AS prerequisite_code
    UNION ALL
    SELECT 'TAD401' AS subject_code, 'TAD301' AS prerequisite_code
    UNION ALL
    SELECT 'TAD401' AS subject_code, 'TAD302' AS prerequisite_code
    UNION ALL
    SELECT 'TAD402' AS subject_code, 'TAD304' AS prerequisite_code
    UNION ALL
    SELECT 'TAD403' AS subject_code, 'TAD301' AS prerequisite_code
    UNION ALL
    SELECT 'TAD403' AS subject_code, 'TAD303' AS prerequisite_code
    UNION ALL
    SELECT 'TAD404' AS subject_code, 'TAD302' AS prerequisite_code
    UNION ALL
    SELECT 'TAD404' AS subject_code, 'TAD304' AS prerequisite_code
    UNION ALL
    SELECT 'TAD405' AS subject_code, 'TAD302' AS prerequisite_code
    UNION ALL
    SELECT 'TAD405' AS subject_code, 'TAD303' AS prerequisite_code
    UNION ALL
    SELECT 'TAD405' AS subject_code, 'TAD305' AS prerequisite_code
) v ON v.subject_code = s.code AND v.prerequisite_code = p.code
WHERE c.code = '10451' AND sp.identifier = '2022.3';

-- ===== 10452 - Técnico en Ciberseguridad (plan 2021.3) =====
INSERT IGNORE INTO careers (code, name, description, status) VALUES ('10452', 'Técnico en Ciberseguridad', 'Carrera técnica de dos años orientada a la protección de sistemas e información.', 'ACTIVE');
INSERT IGNORE INTO study_plans (career_id, identifier, status)
SELECT id, '2021.3', 'CURRENT' FROM careers WHERE code = '10452';

INSERT IGNORE INTO subjects (study_plan_id, code, name, semester, theory_hours, exercise_hours, lab_hours, sct)
SELECT sp.id, v.code, v.name, v.semester, v.t, v.e, v.l, v.sct
FROM study_plans sp JOIN careers c ON c.id = sp.career_id
                    JOIN (
    SELECT 'TCS101' AS code, 'Fundamentos de Programación' AS name, 1 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS102' AS code, 'Fundamentos de Sistemas Computacionales' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS103' AS code, 'Redes de Computadores I' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS104' AS code, 'Sistemas Operativos' AS name, 1 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS105' AS code, 'Matemática Aplicada' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS106' AS code, 'Comunicación Efectiva' AS name, 1 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TCS201' AS code, 'Programación para Ciberseguridad' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS202' AS code, 'Redes de Computadores II' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS203' AS code, 'Administración de Sistemas Linux' AS name, 2 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS204' AS code, 'Fundamentos de Ciberseguridad' AS name, 2 AS semester, 2 AS t, 2 AS e, 2 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS205' AS code, 'Bases de Datos' AS name, 2 AS semester, 2 AS t, 0 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS206' AS code, 'Inglés Técnico' AS name, 2 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
    UNION ALL
    SELECT 'TCS301' AS code, 'Seguridad de Redes' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS302' AS code, 'Seguridad de Sistemas Operativos' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS303' AS code, 'Criptografía Aplicada' AS name, 3 AS semester, 2 AS t, 2 AS e, 2 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS304' AS code, 'Seguridad de Aplicaciones Web' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS305' AS code, 'Monitoreo y Detección de Amenazas' AS name, 3 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS306' AS code, 'Legislación y Ética en Ciberseguridad' AS name, 3 AS semester, 2 AS t, 2 AS e, 0 AS l, 4 AS sct
    UNION ALL
    SELECT 'TCS401' AS code, 'Ethical Hacking y Pruebas de Penetración' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS402' AS code, 'Gestión de Incidentes de Ciberseguridad' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS403' AS code, 'Seguridad en Cloud' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS404' AS code, 'Análisis Forense Digital' AS name, 4 AS semester, 2 AS t, 0 AS e, 4 AS l, 5 AS sct
    UNION ALL
    SELECT 'TCS405' AS code, 'Proyecto de Ciberseguridad' AS name, 4 AS semester, 0 AS t, 0 AS e, 6 AS l, 6 AS sct
    UNION ALL
    SELECT 'TCS406' AS code, 'Gestión de Riesgos de Ciberseguridad' AS name, 4 AS semester, 2 AS t, 2 AS e, 0 AS l, 3 AS sct
) v
WHERE c.code = '10452' AND sp.identifier = '2021.3';

INSERT IGNORE INTO prerequisites (subject_id, prerequisite_id)
SELECT s.id, p.id
FROM subjects s
         JOIN subjects p ON p.study_plan_id = s.study_plan_id
         JOIN study_plans sp ON sp.id = s.study_plan_id
         JOIN careers c ON c.id = sp.career_id
         JOIN (
    SELECT 'TCS201' AS subject_code, 'TCS101' AS prerequisite_code
    UNION ALL
    SELECT 'TCS202' AS subject_code, 'TCS103' AS prerequisite_code
    UNION ALL
    SELECT 'TCS203' AS subject_code, 'TCS104' AS prerequisite_code
    UNION ALL
    SELECT 'TCS204' AS subject_code, 'TCS102' AS prerequisite_code
    UNION ALL
    SELECT 'TCS204' AS subject_code, 'TCS103' AS prerequisite_code
    UNION ALL
    SELECT 'TCS205' AS subject_code, 'TCS101' AS prerequisite_code
    UNION ALL
    SELECT 'TCS301' AS subject_code, 'TCS202' AS prerequisite_code
    UNION ALL
    SELECT 'TCS301' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS302' AS subject_code, 'TCS203' AS prerequisite_code
    UNION ALL
    SELECT 'TCS302' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS303' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS304' AS subject_code, 'TCS201' AS prerequisite_code
    UNION ALL
    SELECT 'TCS304' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS305' AS subject_code, 'TCS202' AS prerequisite_code
    UNION ALL
    SELECT 'TCS305' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS306' AS subject_code, 'TCS204' AS prerequisite_code
    UNION ALL
    SELECT 'TCS401' AS subject_code, 'TCS301' AS prerequisite_code
    UNION ALL
    SELECT 'TCS401' AS subject_code, 'TCS302' AS prerequisite_code
    UNION ALL
    SELECT 'TCS402' AS subject_code, 'TCS305' AS prerequisite_code
    UNION ALL
    SELECT 'TCS403' AS subject_code, 'TCS301' AS prerequisite_code
    UNION ALL
    SELECT 'TCS403' AS subject_code, 'TCS302' AS prerequisite_code
    UNION ALL
    SELECT 'TCS404' AS subject_code, 'TCS302' AS prerequisite_code
    UNION ALL
    SELECT 'TCS405' AS subject_code, 'TCS301' AS prerequisite_code
    UNION ALL
    SELECT 'TCS405' AS subject_code, 'TCS302' AS prerequisite_code
    UNION ALL
    SELECT 'TCS405' AS subject_code, 'TCS305' AS prerequisite_code
    UNION ALL
    SELECT 'TCS406' AS subject_code, 'TCS204' AS prerequisite_code
) v ON v.subject_code = s.code AND v.prerequisite_code = p.code
WHERE c.code = '10452' AND sp.identifier = '2021.3';