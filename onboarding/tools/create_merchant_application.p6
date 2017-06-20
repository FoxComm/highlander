use v6;
use HTTP::Tinyish;
use JSON::Tiny;

sub get-jwt($h, $org, $host, $email, $pass)
{
    my $content = to-json { email=>$email, password=>$pass, org=>$org};
    my $res = $h.post:
    "https://$host/api/v1/public/login",
    headers => {"Content-Type" => "application/json"},
    content => $content;
    return $res<headers><jwt>;
}

sub create-merchant($h, $host, $jwt, $org, $email, $phone, $website)
{
    my $payload = to-json 
    { 
        merchant_application => 
        {
            business_name => $org,
            email_address => $email,
            phone_number=>$phone, 
            site_url=>$website,
            social_profile => {}
        }
    };

    my $res = $h.post:
    "https://$host/api/v1/onboarding/merchant_applications_full",
    headers => {"Content-Type" => "application/json", "jwt" => $jwt},
    content => $payload;
    return $res;
}

sub MAIN($host, $org, $email)
{
    my $h = HTTP::Tinyish.new(agent => "Mozilla/4.0", verify-ssl=>False);
    my $pass = prompt "Pass: ";
    my $jwt = get-jwt($h, $org, $host, $email, $pass);

    my $merch-org = prompt "Org: ";
    my $merch-email = prompt "Email: ";
    my $merch-phone = prompt "Phone: ";
    my $merch-site = prompt "Site: ";

    my $res = create-merchant($h, $host, $jwt, $merch-org, $merch-email, $merch-phone, $merch-site);
    if $res<success> 
    {
        my $content = from-json $res<content>;
        say "Reference: https://api.foxcommerce.com/application/{$content<reference_number>}";
    } 
    else 
    {
        say "failed, {$res<reason>} : {$res<content>}";
    }
}
