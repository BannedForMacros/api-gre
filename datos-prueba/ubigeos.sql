/*
    DATOS DE PRUEBA - NO EJECUTAR EN LA BASE DE UN CLIENTE

    Este script existe para poder probar en desarrollo la cascada de ubigeos
    (departamento -> provincia -> distrito) de la pantalla de guia de salida.
    Se ejecuta contra db_travel_lab de un entorno de desarrollo.

    Por que hace falta
    ------------------
    El SP pr_ObtieneUbigeo indexa maestrodistrito por la LONGITUD de CodZIP:

        @tipo = 1  ->  departamentos: len(CodZip) = 2
        @tipo = 2  ->  provincias   : len(CodZip) = 4 y left(CodZIP,2) = @ubigeo
        @tipo = 3  ->  distritos    : len(CodZip) = 6 y left(CodZIP,4) = @ubigeo

    Pero maestrodistrito guarda TODO con 6 digitos: el departamento Amazonas es
    '010000' y la provincia Chachapoyas es '010100', no '01' ni '0101'. Por eso
    los niveles 1 y 2 devuelven vacio y la cascada nunca arranca, aunque la
    tabla tenga los 1858 distritos reales del pais.

    Este script NO corrige el SP ni toca ninguna fila existente: solo AGREGA las
    filas indice de 2 y de 4 digitos que al SP le faltan para poder responder.

    Los codigos son ubigeos reales del INEI. Los bloques 1 y 3 los derivan de
    las propias filas de resumen que ya tiene la tabla, asi que respetan los
    nombres del ERP. Los bloques 2, 4 y 5 son una copia literal de los mismos
    ubigeos, sin tildes, y solo entran si los de resumen no estuvieran: sirven
    para que el script tambien funcione sobre una base recien creada.

    Es idempotente: todos los MERGE son WHEN NOT MATCHED THEN INSERT, sin rama
    WHEN MATCHED, de modo que correrlo dos veces no duplica nada y nunca
    modifica una fila que ya existia.

    Compatible con SQL Server 2008 / compatibility_level 100: sin THROW, sin
    SEQUENCE, sin OFFSET/FETCH y sin STRING_AGG.
*/

-- sqlcmd abre la sesion con estas dos opciones en OFF, y en OFF los metodos
-- XML y los indices filtrados revientan. Se fijan antes de nada.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

USE db_travel_lab;
GO

SET NOCOUNT ON;
GO

-- Marca de los registros que crea este script, para poder distinguir a simple
-- vista lo sembrado en desarrollo de lo que trajo el ERP.
DECLARE @usuario varchar(15);
SET @usuario = 'DEMOGRE';

BEGIN TRY
    BEGIN TRAN;

    ------------------------------------------------------------------
    -- 1. Departamentos derivados de las filas de resumen '__0000'
    ------------------------------------------------------------------
    MERGE maestrodistrito AS destino
    USING (
        SELECT DISTINCT
               LEFT(CodZIP, 2)  AS cod,
               CodDepartamento  AS departamento
        FROM   maestrodistrito
        WHERE  CodZIP LIKE '[0-9][0-9]0000'
    ) AS origen
        ON destino.CodZIP = origen.cod
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (CodDistrito, CodZIP, CodRegion, CodDepartamento, CodProvincia,
                Estado, UsuarioCreador, FechaCreacion, CodPais)
        VALUES ('', origen.cod, '', origen.departamento, '',
                '1', @usuario, GETDATE(), 'Peru');

    ------------------------------------------------------------------
    -- 2. Los 25 departamentos del Peru, por si el bloque 1 no encontro
    --    filas de resumen (base vacia).
    ------------------------------------------------------------------
    MERGE maestrodistrito AS destino
    USING (
        SELECT '01' AS cod, 'Amazonas'      AS departamento UNION ALL
        SELECT '02', 'Ancash'        UNION ALL
        SELECT '03', 'Apurimac'      UNION ALL
        SELECT '04', 'Arequipa'      UNION ALL
        SELECT '05', 'Ayacucho'      UNION ALL
        SELECT '06', 'Cajamarca'     UNION ALL
        SELECT '07', 'Callao'        UNION ALL
        SELECT '08', 'Cusco'         UNION ALL
        SELECT '09', 'Huancavelica'  UNION ALL
        SELECT '10', 'Huanuco'       UNION ALL
        SELECT '11', 'Ica'           UNION ALL
        SELECT '12', 'Junin'         UNION ALL
        SELECT '13', 'La Libertad'   UNION ALL
        SELECT '14', 'Lambayeque'    UNION ALL
        SELECT '15', 'Lima'          UNION ALL
        SELECT '16', 'Loreto'        UNION ALL
        SELECT '17', 'Madre de Dios' UNION ALL
        SELECT '18', 'Moquegua'      UNION ALL
        SELECT '19', 'Pasco'         UNION ALL
        SELECT '20', 'Piura'         UNION ALL
        SELECT '21', 'Puno'          UNION ALL
        SELECT '22', 'San Martin'    UNION ALL
        SELECT '23', 'Tacna'         UNION ALL
        SELECT '24', 'Tumbes'        UNION ALL
        SELECT '25', 'Ucayali'
    ) AS origen
        ON destino.CodZIP = origen.cod
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (CodDistrito, CodZIP, CodRegion, CodDepartamento, CodProvincia,
                Estado, UsuarioCreador, FechaCreacion, CodPais)
        VALUES ('', origen.cod, '', origen.departamento, '',
                '1', @usuario, GETDATE(), 'Peru');

    ------------------------------------------------------------------
    -- 3. Provincias derivadas de las filas de resumen '____00'
    ------------------------------------------------------------------
    MERGE maestrodistrito AS destino
    USING (
        SELECT DISTINCT
               LEFT(CodZIP, 4)  AS cod,
               CodDepartamento  AS departamento,
               CodProvincia     AS provincia
        FROM   maestrodistrito
        WHERE  CodZIP LIKE '[0-9][0-9][0-9][0-9]00'
          AND  CodZIP NOT LIKE '[0-9][0-9]0000'
    ) AS origen
        ON destino.CodZIP = origen.cod
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (CodDistrito, CodZIP, CodRegion, CodDepartamento, CodProvincia,
                Estado, UsuarioCreador, FechaCreacion, CodPais)
        VALUES ('', origen.cod, '', origen.departamento, origen.provincia,
                '1', @usuario, GETDATE(), 'Peru');

    ------------------------------------------------------------------
    -- 4. Provincias de Lima, Arequipa y La Libertad como respaldo del
    --    bloque 3: son los tres departamentos con los que se prueba la
    --    cascada, asi que tienen que existir si o si.
    ------------------------------------------------------------------
    MERGE maestrodistrito AS destino
    USING (
        SELECT '1501' AS cod, 'Lima' AS departamento, 'Lima' AS provincia UNION ALL
        SELECT '1502', 'Lima', 'Barranca'    UNION ALL
        SELECT '1503', 'Lima', 'Cajatambo'   UNION ALL
        SELECT '1504', 'Lima', 'Canta'       UNION ALL
        SELECT '1505', 'Lima', 'Canete'      UNION ALL
        SELECT '1506', 'Lima', 'Huaral'      UNION ALL
        SELECT '1507', 'Lima', 'Huarochiri'  UNION ALL
        SELECT '1508', 'Lima', 'Huaura'      UNION ALL
        SELECT '1509', 'Lima', 'Oyon'        UNION ALL
        SELECT '1510', 'Lima', 'Yauyos'      UNION ALL

        SELECT '0401', 'Arequipa', 'Arequipa'    UNION ALL
        SELECT '0402', 'Arequipa', 'Camana'      UNION ALL
        SELECT '0403', 'Arequipa', 'Caraveli'    UNION ALL
        SELECT '0404', 'Arequipa', 'Castilla'    UNION ALL
        SELECT '0405', 'Arequipa', 'Caylloma'    UNION ALL
        SELECT '0406', 'Arequipa', 'Condesuyos'  UNION ALL
        SELECT '0407', 'Arequipa', 'Islay'       UNION ALL
        SELECT '0408', 'Arequipa', 'La Union'    UNION ALL

        SELECT '1301', 'La Libertad', 'Trujillo'           UNION ALL
        SELECT '1302', 'La Libertad', 'Ascope'             UNION ALL
        SELECT '1303', 'La Libertad', 'Bolivar'            UNION ALL
        SELECT '1304', 'La Libertad', 'Chepen'             UNION ALL
        SELECT '1305', 'La Libertad', 'Julcan'             UNION ALL
        SELECT '1306', 'La Libertad', 'Otuzco'             UNION ALL
        SELECT '1307', 'La Libertad', 'Pacasmayo'          UNION ALL
        SELECT '1308', 'La Libertad', 'Pataz'              UNION ALL
        SELECT '1309', 'La Libertad', 'Sanchez Carrion'    UNION ALL
        SELECT '1310', 'La Libertad', 'Santiago de Chuco'  UNION ALL
        SELECT '1311', 'La Libertad', 'Gran Chimu'         UNION ALL
        SELECT '1312', 'La Libertad', 'Viru'
    ) AS origen
        ON destino.CodZIP = origen.cod
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (CodDistrito, CodZIP, CodRegion, CodDepartamento, CodProvincia,
                Estado, UsuarioCreador, FechaCreacion, CodPais)
        VALUES ('', origen.cod, '', origen.departamento, origen.provincia,
                '1', @usuario, GETDATE(), 'Peru');

    ------------------------------------------------------------------
    -- 5. Distritos de Lima/Lima, Arequipa/Arequipa y La Libertad/Trujillo.
    --    En una base con datos del ERP ya estan los 1858 del pais y este
    --    bloque no inserta nada; esta para que la cascada tenga hojas
    --    tambien sobre una base vacia.
    ------------------------------------------------------------------
    MERGE maestrodistrito AS destino
    USING (
        SELECT '150101' AS cod, 'Lima' AS departamento, 'Lima' AS provincia, 'Lima' AS distrito UNION ALL
        SELECT '150102', 'Lima', 'Lima', 'Ancon'                    UNION ALL
        SELECT '150103', 'Lima', 'Lima', 'Ate'                      UNION ALL
        SELECT '150104', 'Lima', 'Lima', 'Barranco'                 UNION ALL
        SELECT '150105', 'Lima', 'Lima', 'Brena'                    UNION ALL
        SELECT '150106', 'Lima', 'Lima', 'Carabayllo'               UNION ALL
        SELECT '150107', 'Lima', 'Lima', 'Chaclacayo'               UNION ALL
        SELECT '150108', 'Lima', 'Lima', 'Chorrillos'               UNION ALL
        SELECT '150109', 'Lima', 'Lima', 'Cieneguilla'              UNION ALL
        SELECT '150110', 'Lima', 'Lima', 'Comas'                    UNION ALL
        SELECT '150111', 'Lima', 'Lima', 'El Agustino'              UNION ALL
        SELECT '150112', 'Lima', 'Lima', 'Independencia'            UNION ALL
        SELECT '150113', 'Lima', 'Lima', 'Jesus Maria'              UNION ALL
        SELECT '150114', 'Lima', 'Lima', 'La Molina'                UNION ALL
        SELECT '150115', 'Lima', 'Lima', 'La Victoria'              UNION ALL
        SELECT '150116', 'Lima', 'Lima', 'Lince'                    UNION ALL
        SELECT '150117', 'Lima', 'Lima', 'Los Olivos'               UNION ALL
        SELECT '150118', 'Lima', 'Lima', 'Lurigancho'               UNION ALL
        SELECT '150119', 'Lima', 'Lima', 'Lurin'                    UNION ALL
        SELECT '150120', 'Lima', 'Lima', 'Magdalena del Mar'        UNION ALL
        SELECT '150121', 'Lima', 'Lima', 'Pueblo Libre'             UNION ALL
        SELECT '150122', 'Lima', 'Lima', 'Miraflores'               UNION ALL
        SELECT '150123', 'Lima', 'Lima', 'Pachacamac'               UNION ALL
        SELECT '150124', 'Lima', 'Lima', 'Pucusana'                 UNION ALL
        SELECT '150125', 'Lima', 'Lima', 'Puente Piedra'            UNION ALL
        SELECT '150126', 'Lima', 'Lima', 'Punta Hermosa'            UNION ALL
        SELECT '150127', 'Lima', 'Lima', 'Punta Negra'              UNION ALL
        SELECT '150128', 'Lima', 'Lima', 'Rimac'                    UNION ALL
        SELECT '150129', 'Lima', 'Lima', 'San Bartolo'              UNION ALL
        SELECT '150130', 'Lima', 'Lima', 'San Borja'                UNION ALL
        SELECT '150131', 'Lima', 'Lima', 'San Isidro'               UNION ALL
        SELECT '150132', 'Lima', 'Lima', 'San Juan de Lurigancho'   UNION ALL
        SELECT '150133', 'Lima', 'Lima', 'San Juan de Miraflores'   UNION ALL
        SELECT '150134', 'Lima', 'Lima', 'San Luis'                 UNION ALL
        SELECT '150135', 'Lima', 'Lima', 'San Martin de Porres'     UNION ALL
        SELECT '150136', 'Lima', 'Lima', 'San Miguel'               UNION ALL
        SELECT '150137', 'Lima', 'Lima', 'Santa Anita'              UNION ALL
        SELECT '150138', 'Lima', 'Lima', 'Santa Maria del Mar'      UNION ALL
        SELECT '150139', 'Lima', 'Lima', 'Santa Rosa'               UNION ALL
        SELECT '150140', 'Lima', 'Lima', 'Santiago de Surco'        UNION ALL
        SELECT '150141', 'Lima', 'Lima', 'Surquillo'                UNION ALL
        SELECT '150142', 'Lima', 'Lima', 'Villa El Salvador'        UNION ALL
        SELECT '150143', 'Lima', 'Lima', 'Villa Maria del Triunfo'  UNION ALL

        SELECT '040101', 'Arequipa', 'Arequipa', 'Arequipa'                  UNION ALL
        SELECT '040102', 'Arequipa', 'Arequipa', 'Alto Selva Alegre'         UNION ALL
        SELECT '040103', 'Arequipa', 'Arequipa', 'Cayma'                     UNION ALL
        SELECT '040104', 'Arequipa', 'Arequipa', 'Cerro Colorado'            UNION ALL
        SELECT '040105', 'Arequipa', 'Arequipa', 'Characato'                 UNION ALL
        SELECT '040106', 'Arequipa', 'Arequipa', 'Chiguata'                  UNION ALL
        SELECT '040107', 'Arequipa', 'Arequipa', 'Jacobo Hunter'             UNION ALL
        SELECT '040108', 'Arequipa', 'Arequipa', 'La Joya'                   UNION ALL
        SELECT '040109', 'Arequipa', 'Arequipa', 'Mariano Melgar'            UNION ALL
        SELECT '040110', 'Arequipa', 'Arequipa', 'Miraflores'                UNION ALL
        SELECT '040111', 'Arequipa', 'Arequipa', 'Mollebaya'                 UNION ALL
        SELECT '040112', 'Arequipa', 'Arequipa', 'Paucarpata'                UNION ALL
        SELECT '040113', 'Arequipa', 'Arequipa', 'Pocsi'                     UNION ALL
        SELECT '040114', 'Arequipa', 'Arequipa', 'Polobaya'                  UNION ALL
        SELECT '040115', 'Arequipa', 'Arequipa', 'Quequena'                  UNION ALL
        SELECT '040116', 'Arequipa', 'Arequipa', 'Sabandia'                  UNION ALL
        SELECT '040117', 'Arequipa', 'Arequipa', 'Sachaca'                   UNION ALL
        SELECT '040118', 'Arequipa', 'Arequipa', 'San Juan de Siguas'        UNION ALL
        SELECT '040119', 'Arequipa', 'Arequipa', 'San Juan de Tarucani'      UNION ALL
        SELECT '040120', 'Arequipa', 'Arequipa', 'Santa Isabel de Siguas'    UNION ALL
        SELECT '040121', 'Arequipa', 'Arequipa', 'Santa Rita de Siguas'      UNION ALL
        SELECT '040122', 'Arequipa', 'Arequipa', 'Socabaya'                  UNION ALL
        SELECT '040123', 'Arequipa', 'Arequipa', 'Tiabaya'                   UNION ALL
        SELECT '040124', 'Arequipa', 'Arequipa', 'Uchumayo'                  UNION ALL
        SELECT '040125', 'Arequipa', 'Arequipa', 'Vitor'                     UNION ALL
        SELECT '040126', 'Arequipa', 'Arequipa', 'Yanahuara'                 UNION ALL
        SELECT '040127', 'Arequipa', 'Arequipa', 'Yarabamba'                 UNION ALL
        SELECT '040128', 'Arequipa', 'Arequipa', 'Yura'                      UNION ALL
        SELECT '040129', 'Arequipa', 'Arequipa', 'Jose Luis Bustamante y Rivero' UNION ALL

        SELECT '130101', 'La Libertad', 'Trujillo', 'Trujillo'               UNION ALL
        SELECT '130102', 'La Libertad', 'Trujillo', 'El Porvenir'            UNION ALL
        SELECT '130103', 'La Libertad', 'Trujillo', 'Florencia de Mora'      UNION ALL
        SELECT '130104', 'La Libertad', 'Trujillo', 'Huanchaco'              UNION ALL
        SELECT '130105', 'La Libertad', 'Trujillo', 'La Esperanza'           UNION ALL
        SELECT '130106', 'La Libertad', 'Trujillo', 'Laredo'                 UNION ALL
        SELECT '130107', 'La Libertad', 'Trujillo', 'Moche'                  UNION ALL
        SELECT '130108', 'La Libertad', 'Trujillo', 'Poroto'                 UNION ALL
        SELECT '130109', 'La Libertad', 'Trujillo', 'Salaverry'              UNION ALL
        SELECT '130110', 'La Libertad', 'Trujillo', 'Simbal'                 UNION ALL
        SELECT '130111', 'La Libertad', 'Trujillo', 'Victor Larco Herrera'
    ) AS origen
        ON destino.CodZIP = origen.cod
    WHEN NOT MATCHED BY TARGET THEN
        INSERT (CodDistrito, CodZIP, CodRegion, CodDepartamento, CodProvincia,
                Estado, UsuarioCreador, FechaCreacion, CodPais)
        VALUES (origen.distrito, origen.cod, '', origen.departamento, origen.provincia,
                '1', @usuario, GETDATE(), 'Peru');

    COMMIT TRAN;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRAN;
    -- Sin THROW: no existe en SQL Server 2008. RAISERROR con la severidad
    -- original recortada a 18, que es la maxima que no exige sysadmin.
    DECLARE @msg nvarchar(2048);
    SET @msg = ERROR_MESSAGE();
    RAISERROR(@msg, 16, 1);
END CATCH;
GO

-- Resumen de lo que quedo cargado, para verificar de un vistazo.
SELECT 'departamentos (len 2)' AS nivel, COUNT(*) AS filas
FROM   maestrodistrito WHERE LEN(CodZIP) = 2
UNION ALL
SELECT 'provincias (len 4)', COUNT(*)
FROM   maestrodistrito WHERE LEN(CodZIP) = 4
UNION ALL
SELECT 'distritos (len 6)', COUNT(*)
FROM   maestrodistrito WHERE LEN(CodZIP) = 6;
GO

-- Prueba rapida de la cascada por los tres niveles del SP.
EXEC pr_ObtieneUbigeo 1, NULL;
EXEC pr_ObtieneUbigeo 2, '15';
EXEC pr_ObtieneUbigeo 3, '1501';
GO
