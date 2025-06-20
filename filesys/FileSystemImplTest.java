package filesys;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import exception.CaminhoJaExistenteException;
import exception.CaminhoNaoEncontradoException;
import exception.PermissaoException;

import java.util.Arrays;

public class FileSystemImplTest {

    private static final String ROOT_USER = "root";
    private static final String TEST_USER = "testuser";
    private static final String OTHER_USER = "otheruser";

    private IFileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = new FileSystemImpl();

        // Adicionar usuários de teste
        ((FileSystemImpl) fileSystem).addUser(TEST_USER);
        ((FileSystemImpl) fileSystem).addUser(OTHER_USER);
    }

// ==================== TESTES PARA MKDIR ====================

    @Test
    public void testMkdirSuccess() throws Exception {
        // Teste básico de criação de diretório
        fileSystem.mkdir("/testdir", ROOT_USER);

        // Verificar se o diretório foi criado (usando ls)
        // Como não podemos verificar diretamente, usamos ls e capturamos a saída
        // Ou podemos tentar criar novamente e esperar uma exceção
        try {
            fileSystem.mkdir("/testdir", ROOT_USER);
            fail("Deveria lançar CaminhoJaExistenteException");
        } catch (CaminhoJaExistenteException e) {
            // Esperado
        }
    }

    @Test
    public void testMkdirNestedSuccess() throws Exception {
        // Criar diretório aninhado
        fileSystem.mkdir("/parent", ROOT_USER);
        fileSystem.mkdir("/parent/child", ROOT_USER);

        // Verificar se o diretório foi criado
        try {
            fileSystem.mkdir("/parent/child", ROOT_USER);
            fail("Deveria lançar CaminhoJaExistenteException");
        } catch (CaminhoJaExistenteException e) {
            // Esperado
        }
    }

    @Test(expected = CaminhoJaExistenteException.class)
    public void testMkdirAlreadyExists() throws Exception {
        fileSystem.mkdir("/testdir", ROOT_USER);
        fileSystem.mkdir("/testdir", ROOT_USER); // Deve lançar exceção
    }

    @Test(expected = PermissaoException.class)
    public void testMkdirNoPermission() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões para não permitir escrita
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar criar diretório sem permissão
        fileSystem.mkdir("/restricted/subdir", TEST_USER); // Deve lançar exceção
    }

    @Test(expected = PermissaoException.class)
    public void testMkdirParentNotFound() throws Exception {
        // Tentar criar diretório em um caminho que não existe
        fileSystem.mkdir("/nonexistent/testdir", ROOT_USER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMkdirNullPath() throws Exception {
        fileSystem.mkdir(null, ROOT_USER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMkdirEmptyPath() throws Exception {
        fileSystem.mkdir("", ROOT_USER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMkdirNullUser() throws Exception {
        fileSystem.mkdir("/testdir", null);
    }

    @Test(expected = PermissaoException.class)
    public void testMkdirNonexistentUser() throws Exception {
        fileSystem.mkdir("/testdir", "nonexistentuser");
    }

// ==================== TESTES PARA CHMOD ====================

    @Test
    public void testChmodSuccess() throws Exception {
        // Criar diretório
        fileSystem.mkdir("/testdir", ROOT_USER);

        // Alterar permissões
        fileSystem.chmod("/testdir", ROOT_USER, TEST_USER, "rwx");

        // Verificar se as permissões foram alteradas (criar um subdiretório para testar)
        fileSystem.mkdir("/testdir/subdir", TEST_USER);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testChmodPathNotFound() throws Exception {
        fileSystem.chmod("/nonexistent", ROOT_USER, TEST_USER, "rwx");
    }

    @Test(expected = PermissaoException.class)
    public void testChmodNoPermission() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/testdir", ROOT_USER);

        // Tentar alterar permissões sem ser root ou dono
        fileSystem.chmod("/testdir", TEST_USER, OTHER_USER, "rwx");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChmodInvalidPermission() throws Exception {
        fileSystem.mkdir("/testdir", ROOT_USER);
        fileSystem.chmod("/testdir", ROOT_USER, TEST_USER, "invalid");
    }

    @Test
    public void testChmodAsOwner() throws Exception {
        // Criar diretório com TEST_USER
        fileSystem.mkdir("/userdir", TEST_USER);

        // Alterar permissões como dono
        fileSystem.chmod("/userdir", TEST_USER, OTHER_USER, "r--");

        // Verificar se as permissões foram alteradas (tentar escrever e esperar exceção)
        try {
            fileSystem.mkdir("/userdir/subdir", OTHER_USER);
            fail("Deveria lançar PermissaoException");
        } catch (PermissaoException e) {
            // Esperado
        }
    }

// ==================== TESTES PARA RM ====================

    @Test
    public void testRmFileSuccess() throws Exception {
        // Criar arquivo
        fileSystem.touch("/testfile", ROOT_USER);

        // Remover arquivo
        fileSystem.rm("/testfile", ROOT_USER, false);

        // Verificar se o arquivo foi removido
        try {
            byte[] buffer = new byte[10];
            fileSystem.read("/testfile", ROOT_USER, buffer);
            fail("Deveria lançar CaminhoNaoEncontradoException");
        } catch (CaminhoNaoEncontradoException e) {
            // Esperado
        }
    }

    @Test
    public void testRmDirectorySuccess() throws Exception {
        // Criar diretório
        fileSystem.mkdir("/testdir", ROOT_USER);

        // Remover diretório
        fileSystem.rm("/testdir", ROOT_USER, false);

        // Verificar se o diretório foi removido
        try {
            fileSystem.ls("/testdir", ROOT_USER, false);
            fail("Deveria lançar CaminhoNaoEncontradoException");
        } catch (CaminhoNaoEncontradoException e) {
            // Esperado
        }
    }

    @Test
    public void testRmRecursiveSuccess() throws Exception {
        // Criar estrutura de diretórios
        fileSystem.mkdir("/parent", ROOT_USER);
        fileSystem.mkdir("/parent/child", ROOT_USER);
        fileSystem.touch("/parent/file", ROOT_USER);

        // Remover recursivamente
        fileSystem.rm("/parent", ROOT_USER, true);

        // Verificar se foi removido
        try {
            fileSystem.ls("/parent", ROOT_USER, false);
            fail("Deveria lançar CaminhoNaoEncontradoException");
        } catch (CaminhoNaoEncontradoException e) {
            // Esperado
        }
    }

    @Test(expected = PermissaoException.class)
    public void testRmNonEmptyDirectoryNonRecursive() throws Exception {
        // Criar estrutura de diretórios
        fileSystem.mkdir("/parent", ROOT_USER);
        fileSystem.mkdir("/parent/child", ROOT_USER);

        // Tentar remover sem recursão
        fileSystem.rm("/parent", ROOT_USER, false);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testRmPathNotFound() throws Exception {
        fileSystem.rm("/nonexistent", ROOT_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testRmNoPermission() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões para não permitir escrita
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar remover sem permissão
        fileSystem.rm("/restricted", TEST_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testRmRootDirectory() throws Exception {
        fileSystem.rm("/", ROOT_USER, true);
    }

// ==================== TESTES PARA TOUCH ====================

    @Test
    public void testTouchSuccess() throws Exception {
        // Criar arquivo
        fileSystem.touch("/testfile", ROOT_USER);

        // Verificar se o arquivo foi criado
        byte[] buffer = new byte[0];
        fileSystem.read("/testfile", ROOT_USER, buffer);
    }

    @Test
    public void testTouchInSubdirectory() throws Exception {
        // Criar diretório
        fileSystem.mkdir("/testdir", ROOT_USER);

        // Criar arquivo no diretório
        fileSystem.touch("/testdir/testfile", ROOT_USER);

        // Verificar se o arquivo foi criado
        byte[] buffer = new byte[0];
        fileSystem.read("/testdir/testfile", ROOT_USER, buffer);
    }

    @Test(expected = CaminhoJaExistenteException.class)
    public void testTouchFileAlreadyExists() throws Exception {
        fileSystem.touch("/testfile", ROOT_USER);
        fileSystem.touch("/testfile", ROOT_USER);
    }

    @Test(expected = CaminhoJaExistenteException.class)
    public void testTouchDirectoryExists() throws Exception {
        fileSystem.mkdir("/testdir", ROOT_USER);
        fileSystem.touch("/testdir", ROOT_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testTouchNoPermission() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões para não permitir escrita
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar criar arquivo sem permissão
        fileSystem.touch("/restricted/testfile", TEST_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testTouchParentNotFound() throws Exception {
        fileSystem.touch("/nonexistent/testfile", ROOT_USER);
    }

// ==================== TESTES PARA WRITE ====================

    @Test
    public void testWriteSuccess() throws Exception {
        // Criar arquivo
        fileSystem.touch("/testfile", ROOT_USER);

        // Escrever dados
        byte[] dataToWrite = "Hello, World!".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, dataToWrite);

        // Verificar se os dados foram escritos
        byte[] buffer = new byte[20];
        fileSystem.read("/testfile", ROOT_USER, buffer);

        byte[] expected = Arrays.copyOf(dataToWrite, dataToWrite.length);
        byte[] actual = Arrays.copyOf(buffer, dataToWrite.length);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testWriteAppend() throws Exception {
        // Criar arquivo
        fileSystem.touch("/testfile", ROOT_USER);

        // Escrever dados iniciais
        byte[] initialData = "Hello, ".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, initialData);

        // Anexar mais dados
        byte[] additionalData = "World!".getBytes();
        fileSystem.write("/testfile", ROOT_USER, true, additionalData);

        // Verificar se os dados foram anexados
        byte[] buffer = new byte[20];
        fileSystem.read("/testfile", ROOT_USER, buffer);

        byte[] expected = "Hello, World!".getBytes();
        byte[] actual = Arrays.copyOf(buffer, expected.length);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testWriteOverwrite() throws Exception {
        // Criar arquivo
        fileSystem.touch("/testfile", ROOT_USER);

        // Escrever dados iniciais
        byte[] initialData = "Initial content".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, initialData);

        // Sobrescrever dados
        byte[] newData = "New content".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, newData);

        // Verificar se os dados foram sobrescritos
        byte[] buffer = new byte[20];
        fileSystem.read("/testfile", ROOT_USER, buffer);

        byte[] expected = newData;
        byte[] actual = Arrays.copyOf(buffer, newData.length);

        assertArrayEquals(expected, actual);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testWriteFileNotFound() throws Exception {
        byte[] data = "Test".getBytes();
        fileSystem.write("/nonexistent", ROOT_USER, false, data);
    }

    @Test(expected = PermissaoException.class)
    public void testWriteNoPermission() throws Exception {
        // Criar arquivo com root
        fileSystem.touch("/restricted", ROOT_USER);

        // Configurar permissões para não permitir escrita
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar escrever sem permissão
        byte[] data = "Test".getBytes();
        fileSystem.write("/restricted", TEST_USER, false, data);
    }

// ==================== TESTES PARA READ ====================

    @Test
    public void testReadSuccess() throws Exception {
        // Criar arquivo e escrever dados
        fileSystem.touch("/testfile", ROOT_USER);
        byte[] dataToWrite = "Test data".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, dataToWrite);

        // Ler dados
        byte[] buffer = new byte[20];
        fileSystem.read("/testfile", ROOT_USER, buffer);

        // Verificar dados lidos
        byte[] expected = dataToWrite;
        byte[] actual = Arrays.copyOf(buffer, dataToWrite.length);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadEmptyFile() throws Exception {
        // Criar arquivo vazio
        fileSystem.touch("/emptyfile", ROOT_USER);

        // Ler dados
        byte[] buffer = new byte[10];
        fileSystem.read("/emptyfile", ROOT_USER, buffer);

        // Verificar que o buffer está vazio
        byte[] expected = new byte[0];
        byte[] actual = Arrays.copyOf(buffer, 0);

        assertArrayEquals(expected, actual);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testReadFileNotFound() throws Exception {
        byte[] buffer = new byte[10];
        fileSystem.read("/nonexistent", ROOT_USER, buffer);
    }

    @Test(expected = PermissaoException.class)
    public void testReadNoPermission() throws Exception {
        // Criar arquivo com root
        fileSystem.touch("/restricted", ROOT_USER);

        // Configurar permissões para não permitir leitura
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "-wx");

        // Tentar ler sem permissão
        byte[] buffer = new byte[10];
        fileSystem.read("/restricted", TEST_USER, buffer);
    }

    @Test
    public void testReadBufferSmallerThanContent() throws Exception {
        // Criar arquivo e escrever dados
        fileSystem.touch("/testfile", ROOT_USER);
        byte[] dataToWrite = "This is a long string that won't fit in a small buffer".getBytes();
        fileSystem.write("/testfile", ROOT_USER, false, dataToWrite);

        // Ler com buffer menor
        byte[] smallBuffer = new byte[10];
        fileSystem.read("/testfile", ROOT_USER, smallBuffer);

        // Verificar que apenas parte dos dados foi lida
        byte[] expected = Arrays.copyOf(dataToWrite, 10);
        assertArrayEquals(expected, smallBuffer);
    }

// ==================== TESTES PARA MV ====================

    @Test
    public void testMvFileSuccess() throws Exception {
        // Criar arquivo e escrever dados
        fileSystem.touch("/sourcefile", ROOT_USER);
        byte[] data = "Test data".getBytes();
        fileSystem.write("/sourcefile", ROOT_USER, false, data);

        // Mover arquivo
        fileSystem.mv("/sourcefile", "/destfile", ROOT_USER);

        // Verificar que o arquivo foi movido
        try {
            byte[] buffer = new byte[10];
            fileSystem.read("/sourcefile", ROOT_USER, buffer);
            fail("Deveria lançar CaminhoNaoEncontradoException");
        } catch (CaminhoNaoEncontradoException e) {
            // Esperado
        }

        // Verificar que o conteúdo foi preservado
        byte[] buffer = new byte[20];
        fileSystem.read("/destfile", ROOT_USER, buffer);

        byte[] expected = data;
        byte[] actual = Arrays.copyOf(buffer, data.length);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testMvDirectorySuccess() throws Exception {
        // Criar estrutura de diretórios
        fileSystem.mkdir("/sourcedir", ROOT_USER);
        fileSystem.touch("/sourcedir/file", ROOT_USER);

        // Mover diretório
        fileSystem.mv("/sourcedir", "/destdir", ROOT_USER);

        // Verificar que o diretório foi movido
        try {
            fileSystem.ls("/sourcedir", ROOT_USER, false);
            fail("Deveria lançar CaminhoNaoEncontradoException");
        } catch (CaminhoNaoEncontradoException e) {
            // Esperado
        }

        // Verificar que o conteúdo foi preservado
        byte[] buffer = new byte[0];
        fileSystem.read("/destdir/file", ROOT_USER, buffer);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testMvSourceNotFound() throws Exception {
        fileSystem.mv("/nonexistent", "/dest", ROOT_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testMvNoPermissionSource() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/restricted", ROOT_USER);
        fileSystem.touch("/restricted/file", ROOT_USER);

        // Configurar permissões para não permitir escrita
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar mover sem permissão
        fileSystem.mv("/restricted/file", "/dest", TEST_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testMvNoPermissionDest() throws Exception {
        // Criar estrutura
        fileSystem.mkdir("/source", ROOT_USER);
        fileSystem.touch("/source/file", ROOT_USER);
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões
        fileSystem.chmod("/source", ROOT_USER, TEST_USER, "rwx");
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar mover sem permissão no destino
        fileSystem.mv("/source/file", "/restricted/file", TEST_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testMvRootDirectory() throws Exception {
        fileSystem.mv("/", "/newroot", ROOT_USER);
    }

    @Test(expected = PermissaoException.class)
    public void testMvDestinationExists() throws Exception {
        // Criar arquivos
        fileSystem.touch("/source", ROOT_USER);
        fileSystem.touch("/dest", ROOT_USER);

        // Tentar mover para destino que já existe
        fileSystem.mv("/source", "/dest", ROOT_USER);
    }

// ==================== TESTES PARA LS ====================

    @Test
    public void testLsEmptyDirectory() throws Exception {
        // Criar diretório vazio
        fileSystem.mkdir("/emptydir", ROOT_USER);

        // Listar diretório (não podemos verificar a saída diretamente, mas podemos verificar que não lança exceção)
        fileSystem.ls("/emptydir", ROOT_USER, false);
    }

    @Test
    public void testLsNonEmptyDirectory() throws Exception {
        // Criar estrutura
        fileSystem.mkdir("/testdir", ROOT_USER);
        fileSystem.touch("/testdir/file1", ROOT_USER);
        fileSystem.touch("/testdir/file2", ROOT_USER);
        fileSystem.mkdir("/testdir/subdir", ROOT_USER);

        // Listar diretório
        fileSystem.ls("/testdir", ROOT_USER, false);
    }

    @Test
    public void testLsRecursive() throws Exception {
        // Criar estrutura
        fileSystem.mkdir("/testdir", ROOT_USER);
        fileSystem.touch("/testdir/file1", ROOT_USER);
        fileSystem.mkdir("/testdir/subdir", ROOT_USER);
        fileSystem.touch("/testdir/subdir/file2", ROOT_USER);

        // Listar recursivamente
        fileSystem.ls("/testdir", ROOT_USER, true);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testLsDirectoryNotFound() throws Exception {
        fileSystem.ls("/nonexistent", ROOT_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testLsNoPermission() throws Exception {
        // Criar diretório com root
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões para não permitir leitura
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "-wx");

        // Tentar listar sem permissão
        fileSystem.ls("/restricted", TEST_USER, false);
    }

// ==================== TESTES PARA CP ====================

    @Test
    public void testCpFileSuccess() throws Exception {
        // Criar arquivo e escrever dados
        fileSystem.touch("/sourcefile", ROOT_USER);
        byte[] data = "Test data".getBytes();
        fileSystem.write("/sourcefile", ROOT_USER, false, data);

        // Copiar arquivo
        fileSystem.cp("/sourcefile", "/destfile", ROOT_USER, false);

        // Verificar que o arquivo original ainda existe
        byte[] sourceBuffer = new byte[20];
        fileSystem.read("/sourcefile", ROOT_USER, sourceBuffer);

        // Verificar que o conteúdo foi copiado
        byte[] destBuffer = new byte[20];
        fileSystem.read("/destfile", ROOT_USER, destBuffer);

        assertArrayEquals(sourceBuffer, destBuffer);
    }

    @Test
    public void testCpDirectoryRecursive() throws Exception {
        // Criar estrutura
        fileSystem.mkdir("/sourcedir", ROOT_USER);
        fileSystem.touch("/sourcedir/file", ROOT_USER);
        byte[] data = "Test data".getBytes();
        fileSystem.write("/sourcedir/file", ROOT_USER, false, data);
        fileSystem.mkdir("/sourcedir/subdir", ROOT_USER);
        fileSystem.touch("/sourcedir/subdir/file2", ROOT_USER);

        // Copiar diretório recursivamente
        fileSystem.cp("/sourcedir", "/destdir", ROOT_USER, true);

        // Verificar que a estrutura foi copiada
        byte[] buffer = new byte[20];
        fileSystem.read("/destdir/file", ROOT_USER, buffer);

        byte[] expected = data;
        byte[] actual = Arrays.copyOf(buffer, data.length);

        assertArrayEquals(expected, actual);

        // Verificar subdiretório
        byte[] buffer2 = new byte[0];
        fileSystem.read("/destdir/subdir/file2", ROOT_USER, buffer2);
    }

    @Test(expected = PermissaoException.class)
    public void testCpDirectoryNonRecursive() throws Exception {
        // Criar diretório
        fileSystem.mkdir("/sourcedir", ROOT_USER);

        // Tentar copiar sem recursão
        fileSystem.cp("/sourcedir", "/destdir", ROOT_USER, false);
    }

    @Test(expected = CaminhoNaoEncontradoException.class)
    public void testCpSourceNotFound() throws Exception {
        fileSystem.cp("/nonexistent", "/dest", ROOT_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testCpNoReadPermission() throws Exception {
        // Criar arquivo com root
        fileSystem.touch("/restricted", ROOT_USER);

        // Configurar permissões para não permitir leitura
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "-wx");

        // Tentar copiar sem permissão de leitura
        fileSystem.cp("/restricted", "/dest", TEST_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testCpNoWritePermission() throws Exception {
        // Criar estrutura
        fileSystem.touch("/source", ROOT_USER);
        fileSystem.mkdir("/restricted", ROOT_USER);

        // Configurar permissões
        fileSystem.chmod("/source", ROOT_USER, TEST_USER, "r-x");
        fileSystem.chmod("/restricted", ROOT_USER, TEST_USER, "r-x");

        // Tentar copiar sem permissão de escrita no destino
        fileSystem.cp("/source", "/restricted/dest", TEST_USER, false);
    }

    @Test(expected = PermissaoException.class)
    public void testCpDestinationExists() throws Exception {
        // Criar arquivos
        fileSystem.touch("/source", ROOT_USER);
        fileSystem.touch("/dest", ROOT_USER);

        // Tentar copiar para destino que já existe
        fileSystem.cp("/source", "/dest", ROOT_USER, false);
    }

// ==================== TESTES ADICIONAIS ====================

    @Test
    public void testComplexScenario() throws Exception {
        // Criar estrutura complexa
        fileSystem.mkdir("/home", ROOT_USER);
        fileSystem.mkdir("/home/user1", ROOT_USER);
        fileSystem.mkdir("/home/user2", ROOT_USER);

        // Configurar permissões
        fileSystem.chmod("/home/user1", ROOT_USER, TEST_USER, "rwx");
        fileSystem.chmod("/home/user2", ROOT_USER, OTHER_USER, "rwx");

        // Usuário 1 cria arquivos
        fileSystem.touch("/home/user1/file1", TEST_USER);
        byte[] data1 = "User 1 data".getBytes();
        fileSystem.write("/home/user1/file1", TEST_USER, false, data1);

        // Usuário 2 cria arquivos
        fileSystem.touch("/home/user2/file2", OTHER_USER);
        byte[] data2 = "User 2 data".getBytes();
        fileSystem.write("/home/user2/file2", OTHER_USER, false, data2);

        // Usuário 1 tenta acessar arquivos do usuário 2 (deve falhar)
        try {
            byte[] buffer = new byte[20];
            fileSystem.read("/home/user2/file2", TEST_USER, buffer);
            fail("Deveria lançar PermissaoException");
        } catch (PermissaoException e) {
            // Esperado
        }

        // Root pode acessar tudo
        byte[] buffer1 = new byte[20];
        fileSystem.read("/home/user1/file1", ROOT_USER, buffer1);

        byte[] buffer2 = new byte[20];
        fileSystem.read("/home/user2/file2", ROOT_USER, buffer2);

        // Compartilhar arquivo entre usuários
        fileSystem.chmod("/home/user2/file2", OTHER_USER, TEST_USER, "r--");

        // Agora usuário 1 pode ler
        byte[] buffer3 = new byte[20];
        fileSystem.read("/home/user2/file2", TEST_USER, buffer3);

        // Mas não pode escrever
        try {
            fileSystem.write("/home/user2/file2", TEST_USER, false, "Attempt to modify".getBytes());
            fail("Deveria lançar PermissaoException");
        } catch (PermissaoException e) {
            // Esperado
        }
    }
}