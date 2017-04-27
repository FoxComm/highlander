#!/usr/bin/env perl6
use PKafka::Consumer;
use PKafka::Message;
use PKafka::Producer;
use HTTP::Client;

grammar Nginx
{
    rule TOP {^ '[' <date> ']' '"' <host> <cmd> <path> <protocol> '"' <response> .* $}
    regex date { <day> '/' <month> '/' <year> ':' <hour> ':' <minute> ':' <second> ' +' <timezone> }
    token day { <digit>+ }
    token month { <alnum>+ }
    token year { <digit>+ }
    token hour { <digit>+ }
    token minute { <digit>+ }
    token second { <digit>+ }
    token timezone { <digit>+ }
    token host { \S+ }
    regex path { \S+ }
    regex protocol { 'HTTP/1.1'}  
    token response { \d+ }

    proto token cmd {*}
    token cmd:sym<GET> { <sym> }
    token cmd:sym<POST> { <sym> }
};

grammar Hal
{
    rule TOP {<path> '?' <arg>+}
    regex path { \S+ }
    token identifier {(\w || '-')+}
    rule arg { <key=identifier> '=' <value=identifier>? '&'?} 
};

class HalArgs
{
    method identifier($/) { $/.make: ~$/ }
    method arg($/) { $/.make: $<key>.made => $<value>.made}
    method TOP ($/) { $/.make: Map.new($<arg>».made)}
};

my %months = Jan=>1, Feb=>2, Mar=>3, Apr=>4, May=>5, Jun=>6, Jul=>7, Aug=>8, Sep=>9, Oct=>10, Nov=>11, Dec=>12;

sub to-datetime($date)
{
    my Int $mon = %months{$date<month>};
    return DateTime.new(
        year=> Int($date<year>),
        month=> $mon,
        day=> Int($date<day>),
        hour=>Int($date<hour>),
        minute=>Int($date<minute>),
        second=>Int($date<second>),
        timezone=>Int($date<timezone>)
    );
};

sub MAIN ($kafka-host, $kafka-topic, $henhouse-host) 
{
    say "KAFKA: $kafka-host";
    say "TOPIC: $kafka-topic";
    say "HENHOUSE: $henhouse-host";

    my $config = PKafka::Config.new("group.id"=> "hal-test-1");
    my $log = PKafka::Consumer.new( topic=>$kafka-topic, brokers=>$kafka-host, config=>$config);

    my $henhouse = IO::Socket::INET.new(:host($henhouse-host), :port<2003>);

    my $track-supplier = Supplier.new;
    my $track-supply = $track-supplier.Supply;

        $log.messages.tap(-> $msg
        {
            given $msg
            {
                when PKafka::Message
                {
                    my $r = Nginx.parse($msg.payload-str);
                    send-to-henhouse($r, $track-supplier) if $r<cmd>;
                    $log.save-offset($msg);
                }
                when PKafka::EOF
                {
                    say "Messages Consumed { $msg.total-consumed}";
                }
                when PKafka::Error
                {
                    say "Error {$msg.what}";
                    $log.stop;
                }
            }
        });

    $track-supply.tap(-> $msg
    {
        my $stat = "{$msg.key} {$msg.count} {$msg.time}";

        #send to henhouse
        $henhouse.print("$stat\n");
    });

    say "Reading from kakfa...";

    my $log-promise = $log.consume-from-last(partition=>0);

    await $log-promise;
    $track-supplier.done();
}

class TrackMsg
{
    has Int $.count;
    has Str $.key;
    has Int $.time;
}

sub count($supplier, Int $count, Int $time, Str $key)
{
    $supplier.emit(TrackMsg.new(count=> $count, key=> $key, time=>$time))
}

sub track($supplier, $path, Int $time)
{
    my %args = Hal.parse($path, actions=>HalArgs).made;
    return if not %args<ch>:exists;
    return if not %args<sub>:exists;
    return if not %args<v>:exists;
    return if not %args<ob>:exists;
    return if not %args<id>:exists;

    my $channel = %args<ch>;
    my $subject = %args<sub>;
    my $verb = %args<v>;
    my $object = %args<ob>;
    my $object-id = %args<id>;
    my $count = %args<c>:exists ?? val(%args<c>) !! 1;

    return if $count.WHAT === Str;

    my $cluster = 1; #TODO, get these from tracking url

    count($supplier, $count, $time, "track.$channel.$cluster.$object.$object-id.$verb.$subject");
    count($supplier, $count, $time, "track.$channel.$cluster.$object.$object-id.$verb");
    count($supplier, $count, $time, "track.$channel.$cluster.$object.$verb");
    count($supplier, $count, $time, "track.$channel.$object.$object-id.$verb.$subject");
    count($supplier, $count, $time, "track.$channel.$object.$object-id.$verb");
    count($supplier, $count, $time, "track.$channel.$object.$verb");
    count($supplier, $count, $time, "track.$object.$object-id.$verb.$subject");
    count($supplier, $count, $time, "track.$object.$object-id.$verb");
    count($supplier, $count, $time, "track.$object.$verb");
    count($supplier, $count, $time, "track.$verb.$subject");
    count($supplier, $count, $time, "track.$verb");
}

sub send-to-henhouse($r, $supplier)
{
    #only count things river-rock proxies
    return if not $r<host> ~~ m/river\-rock/;

    my $dt = to-datetime($r<date>);
    my Int $time = $dt.posix;

    if $r<path> ~~ m/api\/v1\/hal/
    {
        track($supplier, $r<path>, $time)
    } 
    else 
    {
        count($supplier, 1, $time, "$r<path>.$r<cmd>.$r<response>");
    }
}

