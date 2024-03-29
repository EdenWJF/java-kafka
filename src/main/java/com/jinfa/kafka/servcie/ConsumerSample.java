package com.jinfa.kafka.servcie;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsumerSample {

    public static final String brokerList = "localhost:9092,localhost:9093,localhost:9094";
    public static final String topic = "TestTopic";
    public static final String groupId = "group.demo";
    public static final AtomicBoolean isRunning = new AtomicBoolean(true);

    public static Properties initConfig(){
        Properties props = new Properties();
        props.put("key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("bootstrap.servers", brokerList);
        props.put("group.id", groupId);
        props.put("client.id", "consumer.client.id.demo");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, ConsumerInterceptorTTL.class.getName());
        return props;
    }

    public static void main(String[] args) {
        Properties props = initConfig();
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        try {
            while (isRunning.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                //按照分区消费
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    //遍历该分区下的记录
                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        System.out.println("topic = " + record.topic() + ", partition = "+ record.partition() + ", offset = " + record.offset());
                        System.out.println("key = " + record.key() + ", value = " + record.value());
                    }
                    // 异步提交位移
                    consumer.commitAsync(new OffsetCommitCallback() {
                        // 提交成功回调
                        @Override
                        public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                            System.out.println(offsets);
                        }
                    });
                }

            }
        } catch (Exception e) {
            System.out.println("occur exception: " + e);;
        } finally {
            consumer.close();
        }
    }
}
