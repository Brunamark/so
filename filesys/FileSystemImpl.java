package filesys;

import java.util.ArrayList;
import java.util.List;

import exception.CaminhoJaExistenteException;
import exception.CaminhoNaoEncontradoException;
import exception.PermissaoException;

// Implemente nesta classe o seu código do FileSystem.
// A classe pode ser alterada.
// O construtor, argumentos do construtor podem ser modificados 
// e atributos & métodos privados podem ser adicionados
public final class FileSystemImpl implements IFileSystem {
    private static final String ROOT_USER = "root";
    private Diretorio root;
    private List<String> users = new ArrayList<>(); 

    public FileSystemImpl() {
        this.root = new Diretorio("root", "/");
    }

    @Override
    public void mkdir(String caminho, String nome)
            throws CaminhoJaExistenteException, PermissaoException {
        try {
            Diretorio parent = navigateTo(caminho);

            for (Diretorio dir : parent.getSubDiretorios()) {
                if (dir.getMetadata().getName().equals(nome)) {
                    throw new CaminhoJaExistenteException("Diretório já existe: " + nome);
                }
            }

            String owner = parent.getMetadata().getOwner();
            String permission = parent.getMetadata().getPermissions().getOrDefault(owner, "");

            if (!permission.contains("w")) {
                throw new PermissaoException("Sem permissão de escrita no caminho: " + caminho);
            }

            Diretorio newDirectory = new Diretorio(owner, nome);
            parent.addSubDiretorio(newDirectory);

        } catch (CaminhoNaoEncontradoException e) {
            throw new PermissaoException("Caminho não encontrado: " + caminho);
        }
    }

    @Override
    public void chmod(String caminho, String usuario, String usuarioAlvo, String permissao)
            throws CaminhoNaoEncontradoException, PermissaoException {
        Diretorio dir = navigateTo(caminho);
        String userPermission = dir.getMetadata().getPermissions().getOrDefault(usuario, "");

        if (!usuario.equals(ROOT_USER) && !userPermission.contains("rw")) {
            throw new PermissaoException("Somente root ou dono pode alterar permissões.");
        }

        dir.getMetadata().getPermissions().put(usuarioAlvo, permissao);
    }

    @Override
    public void rm(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String parentPath = caminho.substring(0, caminho.lastIndexOf("/"));
        String nome = caminho.substring(caminho.lastIndexOf("/") + 1);

        Diretorio parent = navigateTo(parentPath.isEmpty() ? "/" : parentPath);

        for (Arquivo arquivo : parent.getArquivos()) {
            if (arquivo.getMetadata().getName().equals(nome)) {
                String permission = parent.getMetadata().getPermissions().getOrDefault(usuario, "");
                if (!permission.contains("w")) {
                    throw new PermissaoException("Sem permissão para remover.");
                }
                parent.getArquivos().remove(arquivo);
                return;
            }
        }

        for (Diretorio sub : parent.getSubDiretorios()) {
            if (sub.getMetadata().getName().equals(nome)) {
                if (!recursivo && (!sub.getArquivos().isEmpty() || !sub.getSubDiretorios().isEmpty())) {
                    throw new PermissaoException("Diretório não está vazio. Use rm recursivo.");
                }
                parent.getSubDiretorios().remove(sub);
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Item não encontrado.");
    }

    @Override
    public void touch(String caminho, String usuario) throws CaminhoJaExistenteException, PermissaoException {
        String parentPath = caminho.substring(0, caminho.lastIndexOf('/'));
        String fileName = caminho.substring(caminho.lastIndexOf('/') + 1);

        try {
            Diretorio parent = navigateTo(parentPath.isEmpty() ? "/" : parentPath);

            for (Arquivo a : parent.getArquivos()) {
                if (a.getMetadata().getName().equals(fileName)) {
                    throw new CaminhoJaExistenteException("Arquivo já existe: " + fileName);
                }
            }

            String permission = parent.getMetadata().getPermissions().getOrDefault(usuario, "");
            if (!permission.contains("w")) {
                throw new PermissaoException("Sem permissão de escrita no diretório.");
            }

            Arquivo novo = new Arquivo(fileName, usuario);
            parent.addFile(novo);

        } catch (CaminhoNaoEncontradoException e) {
            throw new PermissaoException("Caminho não encontrado.");
        }
    }

    @Override
    public void write(String caminho, String usuario, boolean anexar, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        Diretorio dir = navigateTo(caminho.substring(0, caminho.lastIndexOf("/")));
        String fileName = caminho.substring(caminho.lastIndexOf("/") + 1);

        for (Arquivo arquivo : dir.getArquivos()) {
            if (arquivo.getMetadata().getName().equals(fileName)) {
                String permission = arquivo.getMetadata().getPermissions().getOrDefault(usuario, "");
                if (!permission.contains("w")) {
                    throw new PermissaoException("Sem permissão de escrita no arquivo.");
                }

                if (!anexar) {
                    arquivo.getBlocos().clear();
                    arquivo.getMetadata().setSize(0);
                }

                Bloco bloco = new Bloco(buffer.length);
                bloco.setDados(buffer);
                arquivo.addBloco(bloco);
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Arquivo não encontrado.");
    }

    @Override
    public void read(String caminho, String usuario, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        Diretorio dir = navigateTo(caminho.substring(0, caminho.lastIndexOf("/")));
        String fileName = caminho.substring(caminho.lastIndexOf("/") + 1);

        for (Arquivo arquivo : dir.getArquivos()) {
            if (arquivo.getMetadata().getName().equals(fileName)) {
                String permission = arquivo.getMetadata().getPermissions().getOrDefault(usuario, "");
                if (!permission.contains("r")) {
                    throw new PermissaoException("Sem permissão de leitura no arquivo.");
                }

                byte[] data = arquivo.read();
                System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Arquivo não encontrado.");
    }

    @Override
    public void mv(String caminhoAntigo, String caminhoNovo, String usuario)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String sourceParentPath = caminhoAntigo.substring(0, caminhoAntigo.lastIndexOf("/"));
        String sourceName = caminhoAntigo.substring(caminhoAntigo.lastIndexOf("/") + 1);

        String destParentPath = caminhoNovo.substring(0, caminhoNovo.lastIndexOf("/"));
        String destName = caminhoNovo.substring(caminhoNovo.lastIndexOf("/") + 1);

        Diretorio sourceParent = navigateTo(sourceParentPath.isEmpty() ? "/" : sourceParentPath);
        Diretorio destParent = navigateTo(destParentPath.isEmpty() ? "/" : destParentPath);

        String sourcePermission = sourceParent.getMetadata().getPermissions().getOrDefault(usuario, "");
        if (!sourcePermission.contains("w")) {
            throw new PermissaoException("Sem permissão para mover do caminho: " + caminhoAntigo);
        }

        String destPermission = destParent.getMetadata().getPermissions().getOrDefault(usuario, "");
        if (!destPermission.contains("w")) {
            throw new PermissaoException("Sem permissão para mover para o caminho: " + caminhoNovo);
        }

        for (Arquivo arquivo : sourceParent.getArquivos()) {
            if (arquivo.getMetadata().getName().equals(sourceName)) {
                sourceParent.getArquivos().remove(arquivo);
                arquivo.getMetadata().setName(destName);
                destParent.addFile(arquivo);
                return;
            }
        }

        for (Diretorio subDir : sourceParent.getSubDiretorios()) {
            if (subDir.getMetadata().getName().equals(sourceName)) {
                sourceParent.getSubDiretorios().remove(subDir);
                subDir.getMetadata().setName(destName);
                destParent.addSubDiretorio(subDir);
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Item não encontrado no caminho: " + caminhoAntigo);

    }

    @Override
    public void ls(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        Diretorio dir = navigateTo(caminho);
        listarConteudo(dir, caminho, recursivo, 0);
    }

    @Override
    public void cp(String caminhoOrigem, String caminhoDestino, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String sourceParentPath = caminhoOrigem.substring(0, caminhoOrigem.lastIndexOf("/"));
        String sourceName = caminhoOrigem.substring(caminhoOrigem.lastIndexOf("/") + 1);

        String destParentPath = caminhoDestino.substring(0, caminhoDestino.lastIndexOf("/"));
        String destName = caminhoDestino.substring(caminhoDestino.lastIndexOf("/") + 1);

        Diretorio sourceParent = navigateTo(sourceParentPath.isEmpty() ? "/" : sourceParentPath);
        Diretorio destParent = navigateTo(destParentPath.isEmpty() ? "/" : destParentPath);

        String sourcePermission = sourceParent.getMetadata().getPermissions().getOrDefault(usuario, "");
        if (!sourcePermission.contains("r")) {
            throw new PermissaoException("Sem permissão para ler do caminho: " + caminhoOrigem);
        }

        String destPermission = destParent.getMetadata().getPermissions().getOrDefault(usuario, "");
        if (!destPermission.contains("w")) {
            throw new PermissaoException("Sem permissão para escrever no caminho: " + caminhoDestino);
        }

        for (Arquivo arquivo : sourceParent.getArquivos()) {
            if (arquivo.getMetadata().getName().equals(sourceName)) {
                Arquivo novoArquivo = new Arquivo(destName, usuario);
                novoArquivo.setBlocos(new ArrayList<>(arquivo.getBlocos())); 
                destParent.addFile(novoArquivo);
                return;
            }
        }

        for (Diretorio subDir : sourceParent.getSubDiretorios()) {
            if (subDir.getMetadata().getName().equals(sourceName)) {
                if (!recursivo) {
                    throw new PermissaoException("Cópia de diretório requer o modo recursivo.");
                }
                Diretorio novoDiretorio = copyDiretorio(subDir, destName, usuario);
                destParent.addSubDiretorio(novoDiretorio);
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Item não encontrado no caminho: " + caminhoOrigem);
    }

    public void addUser(String user) {
        if (users.contains(user)) {
            throw new UnsupportedOperationException("Usuário já existe: " + user);
        }
        users.add(user);    
    }

    private Diretorio navigateTo(String path) throws CaminhoNaoEncontradoException {
        if (path.equals("/"))
            return root;

        String[] parts = path.split("/");
        Diretorio current = root;

        for (String part : parts) {
            if (part.isEmpty())
                continue;

            boolean found = false;
            for (Diretorio sub : current.getSubDiretorios()) {
                if (sub.getMetadata().getName().equals(part)) {
                    current = sub;
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new CaminhoNaoEncontradoException("Diretório não encontrado: " + part);
            }
        }

        return current;
    }

    private void listarConteudo(Diretorio dir, String caminho, boolean recursivo, int nivel) {
        String indent = "  ".repeat(nivel);
        System.out.println(indent + "[DIR] " + dir.getMetadata().getName());

        for (Arquivo arquivo : dir.getArquivos()) {
            System.out.println(indent + "  [FILE] " + arquivo.getMetadata().getName());
        }

        if (recursivo) {
            for (Diretorio sub : dir.getSubDiretorios()) {
                listarConteudo(sub, caminho + "/" + sub.getMetadata().getName(), true, nivel + 1);
            }
        }
    }

    private Diretorio copyDiretorio(Diretorio source, String newName, String usuario) {
        Diretorio novoDiretorio = new Diretorio(usuario, newName);
    
        for (Arquivo arquivo : source.getArquivos()) {
            Arquivo novoArquivo = new Arquivo(arquivo.getMetadata().getName(), usuario);
            novoArquivo.setBlocos(new ArrayList<>(arquivo.getBlocos())); 
            novoDiretorio.addFile(novoArquivo);
        }
    
        for (Diretorio subDir : source.getSubDiretorios()) {
            Diretorio novoSubDir = copyDiretorio(subDir, subDir.getMetadata().getName(), usuario);
            novoDiretorio.addSubDiretorio(novoSubDir);
        }
    
        return novoDiretorio;
    }
}
