STACK_NAME=$1-LambdaStack

if [ -z "$1" ]
  then
    echo "No STACK_NAME argument supplied"
    exit 1
fi

echo "Deleting the stack..."

aws cloudformation delete-stack --stack-name $STACK_NAME

if [ $? -eq 0 ]; then
  echo "Delete in progress"
  aws cloudformation wait stack-delete-complete --stack-name $STACK_NAME
  echo "Stack deleted successfully"
else
  echo "Failure while deleting stack"
fi