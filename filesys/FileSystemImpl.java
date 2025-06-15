package filesys;

import exception.CaminhoJaExistenteException;
import exception.CaminhoNaoEncontradoException;
import exception.PermissaoException;

// Implemente nesta classe o seu código do FileSystem.
// A classe pode ser alterada.
// O construtor, argumentos do construtor podem ser modificados 
// e atributos & métodos privados podem ser adicionados
public final class FileSystemImpl implements IFileSystem {
    private static final String ROOT_USER = "root"; // pode ser necessário
    private Directory root;

    public FileSystemImpl() {
        this.root = new Directory("root", "/");

    }

    @Override
    public void mkdir(String caminho, String nome)
            throws CaminhoJaExistenteException, PermissaoException {
        try {
            Directory parent = navigateTo(caminho);

            if (parent.getSubDirectories().containsKey(nome)) {
                throw new CaminhoJaExistenteException("Directory already exists: " + nome);
            }

            String owner = parent.getMetadata().getOwner();
            String permission = parent.getMetadata().getPermissions().getOrDefault(owner, "");

            if (!permission.contains("w")) {
                throw new PermissaoException("No write permission on path: " + caminho);
            }

            Directory newDirectory = new Directory(owner, nome);
            parent.getSubDirectories().put(nome, newDirectory);

        } catch (CaminhoNaoEncontradoException e) {
            throw new PermissaoException("Path not found: " + caminho);
        }

    }

    @Override
    public void chmod(String caminho, String usuario, String usuarioAlvo, String permissao)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'chmod'");
    }

    @Override
    public void rm(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'rm'");
    }

    @Override
    public void touch(String caminho, String usuario) throws CaminhoJaExistenteException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'touch'");
    }

    @Override
    public void write(String caminho, String usuario, boolean anexar, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'write'");
    }

    @Override
    public void read(String caminho, String usuario, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'read'");
    }

    @Override
    public void mv(String caminhoAntigo, String caminhoNovo, String usuario)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'mv'");
    }

    @Override
    public void ls(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'ls'");
    }

    @Override
    public void cp(String caminhoOrigem, String caminhoDestino, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        throw new UnsupportedOperationException("Método não implementado 'cp'");
    }

    public void addUser(String user) {
        throw new UnsupportedOperationException("Método não implementado 'addUser'");
    }

    private Directory navigateTo(String path) throws CaminhoNaoEncontradoException {
        if (path.equals("/"))
            return root;

        String[] parts = path.split("/");
        Directory current = root;

        for (String part : parts) {
            if (part.isEmpty())
                continue;
            current = current.getSubDirectories().get(part);
            if (current == null) {
                throw new CaminhoNaoEncontradoException("Diretório não encontrado: " + part);
            }
        }

        return current;
    }

}
