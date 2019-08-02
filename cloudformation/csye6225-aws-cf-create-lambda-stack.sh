#!/bin/sh
#shell script to create AWS network infrastructures


 help_me()
{
	  echo "Usage:-"
	  echo "$0 <Application-Stack-Name> <S3CodeDeployBucket> <User> <domain> <fromemail>"
	  exit
}

  APP_STACK_NAME=$1-LambdaStack
  CODEDEPLOYBUCKET=$2
  USER=$3
  DOMAIN=$4
  FROM=$5


	if [ $# -ne 5 ]
	then
		echo -e "You are missing some parameters"
		help_me
fi

echo $APP_STACK_NAME
echo "Creating stack..."
aws cloudformation create-stack --stack-name $APP_STACK_NAME --template-body file://csye6225-cf-lambda.json \
--parameters ParameterKey=S3CodeDeployBucket,ParameterValue=$CODEDEPLOYBUCKET\
 ParameterKey=CFNUser,ParameterValue=$USER\
 ParameterKey=domain,ParameterValue=$DOMAIN\
 ParameterKey=fromemail,ParameterValue=$FROM\
 --capabilities CAPABILITY_NAMED_IAM

if [ $? -eq 0 ]; then
echo "Creating progress..."
 aws cloudformation wait stack-create-complete --stack-name $APP_STACK_NAME
 	if [ $? -ne 255 ]; then

  		echo "Stack created successfully!!"
  	else
  		echo "Failure while waiting for stack-create-complete !!"
  	fi

else
  echo "Failure while creating stack !!"

fi