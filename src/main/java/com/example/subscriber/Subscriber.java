package com.example.subscriber;

// Importa as classes necessárias para trabalhar com a API JMS e a implementação IBM MQ.
import javax.jms.*;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.JMSC;
import com.ibm.msg.client.wmq.WMQConstants;

public class Subscriber {

    public static void main(String[] args) {
        try {
            // Configurando a fábrica de conexões para se conectar ao IBM MQ
            MQConnectionFactory factory = new MQConnectionFactory();
            
            // Define o hostname do servidor IBM MQ
            factory.setHostName("localhost");

            // Define a porta onde o servidor IBM MQ está ouvindo
            factory.setPort(1515);

            // Define o tipo de transporte como TCP/IP para uma conexão de cliente
            factory.setTransportType(1);

            // Configura o nome do gerenciador de filas do IBM MQ
            factory.setQueueManager("QMSERPRO");
            
            // Configura o canal de comunicação do IBM MQ
            factory.setChannel("ADMIN.CHL");

            // Cria uma conexão usando as configurações da fábrica de conexões
            Connection connection = factory.createConnection();

            // Define um ID de cliente único para a conexão, necessário para assinaturas duráveis
            connection.setClientID("CLIENTE01");

            // Cria uma sessão JMS sem transações e com confirmação automática de mensagens
            Session session = connection.createSession();

            // Cria um tópico com o nome especificado, que será utilizado para receber mensagens
            Topic topic = session.createTopic("topic://TOPICO1");

            // Nome da assinatura durável
            String subscriptionName = "ASSINATURA01";
            
            // Cria um subscriber durável associado ao tópico e nome da assinatura
            TopicSubscriber durableSubscriber = session.createDurableSubscriber(topic, subscriptionName);

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
                            System.out.println("Mensagem recebida: " + textMessage.getText());
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
