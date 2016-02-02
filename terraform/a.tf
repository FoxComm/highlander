provider "google" 
{ 
    account_file = "${file("account.json")}"
    credentials = "${file("account.json")}"
    project = "foxcomm-staging"
    region = "us-central1"
}
