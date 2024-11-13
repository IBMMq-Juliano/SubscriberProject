package com.example.subscriber;

import javax.jms.*;

import org.json.JSONArray;
import org.json.JSONObject;

// Importa as classes necessárias para trabalhar com a API JMS e a implementação IBM MQ.
import com.ibm.mq.jms.MQConnectionFactory;

public class Subscriber {

    public static void main(String[] args) {
        try {
            // Configurando a fábrica de conexões para se conectar ao IBM MQ
            MQConnectionFactory factory = new MQConnectionFactory();
            
            // Define o hostname do servidor IBM MQ
            factory.setHostName("localhost");

            // Define a porta onde o servidor IBM MQ está ouvindo
            factory.setPort(1616);

            // Define o tipo de transporte como TCP/IP para uma conexão de cliente
            factory.setTransportType(1);

            // Configura o nome do gerenciador de filas do IBM MQ
            factory.setQueueManager("QMEXAMPLES");
            
            // Configura o canal de comunicação do IBM MQ
            factory.setChannel("ADMIN.CHL");

            // Cria uma conexão usando as configurações da fábrica de conexões
            Connection connection = factory.createConnection();

            // Define um ID de cliente único para a conexão, necessário para assinaturas duráveis
            connection.setClientID("FILTRO");

            // Cria uma sessão JMS sem transações e com confirmação automática de mensagens
            Session session = connection.createSession();

            // Cria um tópico com o nome especificado, que será utilizado para receber mensagens
            Topic topic = session.createTopic("T_ORDERS");

            // Nome da assinatura durável
            String subscriptionName = "ASSINANTEFILTRO";

            // Cria um subscriber durável associado ao tópico e nome da assinatura
            TopicSubscriber durableSubscriber = session.createDurableSubscriber(topic, subscriptionName);

            // Cria a fila Q_ORDERS para enviar a mensagem
            Queue queueOrders = session.createQueue("Q_ORDERS");

            // Criando a fila Q_COZINHA
            Queue queueCozinha = session.createQueue("Q_COZINHA");
            
            // Cria a fila Q_SALAO para enviar os itens
            Queue queueSalao = session.createQueue("Q_SALAO");

            // Cria a fila Q_FINANCEIRO para enviar os itens
            Queue queueFinanceiro = session.createQueue(("Q_FINANCEIRO"));

            // Cria a fila Q_PRIORITARIA
            Queue queuePrioritaria = session.createQueue(("Q_PRIORITARIA"));


            // Cria os MessageProducers para enviar as mensagens para as filas
            MessageProducer producerFull = session.createProducer(queueOrders);

            // Criando o produtor para COZINHA
            MessageProducer producerCozinha = session.createProducer(queueCozinha);

            // Criando o produtor para o financeiro
            MessageProducer producerFinanceiro = session.createProducer(queueFinanceiro);

            // Criando o produtor para o Salão
            MessageProducer producerSalao = session.createProducer(queueSalao);

            // Criando o cliente prioritario
            MessageProducer producerPrioritario = session.createProducer(queuePrioritaria);

            // Inicia a conexão para começar a receber mensagens
            connection.start();

            // Configura o listener para tratar as mensagens recebidas
            durableSubscriber.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    // Verifica se a mensagem recebida é do tipo TextMessage

                    if (message instanceof TextMessage) {
                        try {
                            // Converte a mensagem para o tipo TextMessage e imprime o conteúdo
                            TextMessage textMessage = (TextMessage) message;
                            System.out.println("Mensagem recebida: "+textMessage.getText());

                            System.out.println("=================================");
                            System.out.println(message.toString());
                            System.out.println("=================================");

                            int priority = message.getJMSPriority();
                            String resposta = message.getJMSReplyTo().toString();
                            
                            // Cria a fila de RESPOSTA
                            Queue queueResposta = session.createQueue(resposta);
                            MessageProducer producerResposta = session.createProducer(queueResposta);

                            System.out.println("Prioridade da mensagem: " + priority);
                            
                            if (priority >=8){
                                producerPrioritario.send(textMessage);
                            }else{
                                // Envia a mensagem completa para a fila Q_ORDERS
                                producerFull.send(textMessage);
                                System.out.println("Mensagem enviada para a fila Q_ORDERS (FILA COMPLETA)");
                            }

                            // Cria um objeto JSON com a variável 'mensagem'
                            JSONObject mensagemJson = new JSONObject(textMessage.getText());

                            // Exibe o objeto JSON criado
                            // System.out.println(mensagemJson.toString());

                            // Verifica se o campo "itens" existe e extrai a string com os itens
                            if (mensagemJson.has("itens")) {
                                // Extrai o campo "itens" como um JSONArray
                                JSONArray itensArray = mensagemJson.getJSONArray("itens");
                                //System.out.println(itensArray.toString());

                                // Converte o JSONArray em uma string
                                String itensString = itensArray.toString();

                                // Cria um TextMessage com a string dos itens
                                TextMessage textMessageItens = session.createTextMessage(itensString);

                                // Envia a string com os itens para a fila Q_COZINHA
                                producerCozinha.send(textMessageItens);
                                System.out.println("Mensagem enviada para a fila Q_COZINHA");
                            }

                            // Extrai o valor de "total" e "pedidoId" do JSON
                            if (mensagemJson.has("total")){
                                Double total = mensagemJson.getDouble("total");
                                String pedidoId = mensagemJson.getString("pedidoId");

                                TextMessage textMessageFinanceiro = session.createTextMessage("{\"total\":"+total+", \"pedidoId\": "+pedidoId+" }");

                                producerFinanceiro.send(textMessageFinanceiro);
                            }
                            
                            // Cria e envia a mensagem para a fila Q_FINANCEIRO com os dados financeiros

                            // Extrai os dados do cliente e itens
                            String nomeCliente = mensagemJson.getString("nomeCliente");
                            TextMessage textMessageNomeCliente = session.createTextMessage("{\"nomeCliente\": "+nomeCliente+"}");

                            producerSalao.send(textMessageNomeCliente);

                            TextMessage textMessageResposta = session.createTextMessage("Mensagem: "+message.getJMSMessageID()+ " enviada");
                            producerResposta.send(textMessageResposta);

                            // Cria e envia a mensagem para a fila Q_SALAO com o nome do cliente e os itens

                        } catch (JMSException e) {
                            // Imprime o stack trace se houver uma exceção ao processar a mensagem
                            e.printStackTrace();
                        }
                    }
                }
            });

            // Imprime uma mensagem indicando que o consumidor está pronto para receber mensagens
            System.out.println("Aguardando mensagens...");

            // Mantém a aplicação em execução para continuar ouvindo as mensagens
            Thread.sleep(999999999);

            // Fecha o subscriber, a sessão e a conexão ao finalizar
            durableSubscriber.close();
            session.close();
            connection.close();

        } catch (JMSException | InterruptedException e) {
            // Imprime o stack trace se houver uma exceção ao configurar ou finalizar a conexão
            e.printStackTrace();
        }
    }
}
