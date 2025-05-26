# 💬 Chat TCP em Java  
**Aplicação de mensageria em tempo real desenvolvida para a disciplina de Redes de Computadores.**

---

<h2 align="left">🖥️ Linguagens e Ferramentas</h2>
<p align="left">
    <a href="https://www.java.com" target="_blank" rel="noreferrer">
        <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" alt="Java" width="40" height="40"/>
    </a>
    <a href="https://docs.oracle.com/en/java/" target="_blank" rel="noreferrer">
        <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/intellij/intellij-original.svg" alt="IntelliJ IDEA" width="40" height="40"/>
    </a>
</p>

---

## 📌 Descrição do Projeto

Este projeto consiste em uma aplicação de **mensageria em tempo real** desenvolvida em **Java**, utilizando **sockets TCP**. O sistema é dividido em duas partes: **Servidor** e **Clientes**. Os usuários podem se conectar simultaneamente ao servidor para trocar mensagens públicas (broadcast) ou privadas, simulando o funcionamento de um mensageiro.

---

## 🎯 Objetivo do Trabalho

- Criar um sistema de chat funcional usando sockets TCP.  
- Implementar um **servidor capaz de lidar com múltiplos clientes simultâneos**.  
- Permitir comunicação entre clientes com:
  - Mensagens públicas (broadcast)  
  - Mensagens privadas  
- Utilizar a **classe `Mensagem` serializável** para encapsular os dados trocados entre cliente e servidor.  
- Praticar conceitos de **concorrência, threads, comunicação de rede e serialização de objetos**.

---

## 🚀 Funcionalidades

### 🖥️ Servidor
- Aceita **conexões simultâneas** de múltiplos clientes.  
- Realiza o **reencaminhamento (broadcast)** de mensagens para todos os clientes conectados.  
- Exibe no console informações de conexões ativas.  
- Gerencia mensagens privadas entre usuários.  
- Trata exceções de entrada/saída de forma robusta.  

### 👤 Cliente
- Envia e recebe mensagens em tempo real via **terminal** ou **interface gráfica (Swing)**.  
- Permite **escolha de nome de usuário**.  
- Exibe o **nome do remetente** em cada mensagem.  
- Exibe mensagens com **data e hora**.  
- Suporte a comandos especiais:

| Comando | Função |
|--------|--------|
| `/privado:<usuario>:<mensagem>` | Envia mensagem privada |
| `/usuarios` | Lista usuários conectados |

---

## 🧱 Classe `Mensagem`

A comunicação é feita com objetos da classe `Mensagem`, que contém:
- Nome do remetente  
- Nome do destinatário (ou `null` para broadcast)  
- Conteúdo da mensagem  
- Data e hora  

O servidor interpreta o conteúdo e decide o roteamento da mensagem com base no destino.

---

## 📌 Como Executar o projeto
1. Faça o **download** ou clone este repositório:  
   ```sh
   git clone https://github.com/seu-usuario/ecommerce-perfumes.git

