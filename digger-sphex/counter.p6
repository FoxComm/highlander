#!/usr/bin/env perl6
use PKafka::Consumer;
use PKafka::Message;
use PKafka::Producer;
use HTTP::Client;

grammar Nginx
{
    rule TOP {^ .* '"' <cmd> <path> <protocol> '"' <response> .* $}
    regex path { '/' <path-elem>? <path>?}
    token path-elem { \S+ }
    regex protocol { 'HTTP/1.1'}  
    token response { \d+ }

    proto token cmd {*}
    token cmd:sym<GET> { <sym> }
    token cmd:sym<POST> { <sym> }
};

sub MAIN () 
{
    my $brokers = "10.240.0.14";
    my $config = PKafka::Config.new("group.id"=> "hal-test-1");
    my $rsyslog = PKafka::Consumer.new( topic=>"nginx", brokers=>$brokers, config=>$config);

    my $henhouse = IO::Socket::INET.new(:host<localhost>, :port<2003>);

    $rsyslog.messages.tap(-> $msg 
    {
        given $msg 
        {
            when PKafka::Message
            {
                my $r = Nginx.parse($msg.payload-str);
                say "MSG: {$msg.payload-str}";
                send-to-henhouse($r, $henhouse) if $r<cmd>;
                $rsyslog.save-offset($msg);
            }
            when PKafka::EOF
            {
                say "Messages Consumed { $msg.total-consumed}";
            }
            when PKafka::Error
            {
                say "Error {$msg.what}";
                $rsyslog.stop;
            }
        }
    });

    my $t1 = $rsyslog.consume-from-last(partition=>0);
    #my $t1 = $rsyslog.consume-from-beginning(partition=>0);

    await $t1;
}

sub send-to-henhouse($r, $henhouse)
{
    my $key = "$r<path>.$r<cmd>.$r<response>";
    my $count = 1;
    my $stat = "{$key} {$count} {DateTime.now.posix}";
    say "HEN: $stat";

    #send to henhouse
    $henhouse.print("$stat\n");
}

