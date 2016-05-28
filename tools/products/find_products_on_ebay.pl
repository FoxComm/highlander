#!/usr/bin/perl

use strict;
use diagnostics;

use eBay::API::Simple::Finding;
use CGI qw / :standard *table /;
use DateTime::Format::ISO8601;

binmode STDOUT, ':utf8';

my $mode = shift; 

my %modes = (title=>1, subtitle=>1);
die "Mode $mode is not valid" if not exists $modes{$mode};


my $app_id = 'MaximKha-3cf5-4d06-869b-23ffc400f9b3';
my $call = eBay::API::Simple::Finding->new({
        appid=> $app_id,
    });

sub print_products($$$)
{
    my ($keyword, $page, $mode) = @_;
    $call->execute( 
        'findItemsByKeywords', 
        { 
            keywords => $keyword, 
            paginationInput => { pageNumber=>$page, entriesPerPage => 100 }, 
            sortOrder=>'StartTimeNewest'
        } );

    if ( $call->has_error() ) {
        die "error: find call failed, check the appid. " . $call->errors_as_string();
    }

    # getters for the response DOM or Hash
    my $dom  = $call->response_dom();
    my @nodes = $dom->findnodes('//item');

    for my $n (@nodes) {
        my %product = (
            id => $n->findvalue('itemId/text()'),
            data => $n->findvalue("$mode/text()"),
        );
        print $product{data} . "\n";
    }
}

foreach my $c (@ARGV) 
{
    foreach my $page (0..10) 
    {
        print_products($c, $page, $mode);
    }
}
