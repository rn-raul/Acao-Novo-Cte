# Ação Novo CT-e

## 📋 Descrição

Este projeto implementa uma **Ação customizada no Sankhya** que se integra à tela de **Controle de Frete** para automatizar a geração de Conhecimento de Transporte Eletrônico (**CT-e**). 

A integração permite que, a partir dos dados fornecidos na tela de Controle de Frete, seja realizada a geração automática do CT-e através da API HiveCloud, simplificando o processo de emissão de documentos fiscais de transporte.

## 🎯 Funcionalidade Principal

- **Geração automática de CT-e**: Captura dados da tela de Controle de Frete do Sankhya
- **Integração HiveCloud**: Comunica-se com a API HiveCloud para processar a geração do CT-e
- **Autenticação segura**: Utiliza Bearer Token e TenantId para autenticação
- **Tratamento de respostas HTTP**: Gerencia respostas de sucesso e erro da API
