# Security Guidelines

This document outlines security best practices for this project. **Never commit sensitive information to this repository.**

## Sensitive Information to Never Commit

- **AWS Credentials**: Access keys, secret keys, or session tokens
- **Environment Variables**: `.env`, `.env.local`, `.env.*.local` files
- **API Keys and Tokens**: Any authentication tokens or API keys
- **Passwords and Secrets**: Database passwords, encryption keys, etc.
- **AWS Account IDs**: While not secret, hardcoding them is bad practice
- **Private Keys**: `.pem`, `.key`, or certificate files

## Pre-Commit Checklist

Before pushing to this repository, verify:

✅ No credentials in code files (Java, JSON, XML, properties)  
✅ No `.env` files or environment variable files  
✅ No AWS credentials files (`~/.aws/credentials`, `.aws-credentials`)  
✅ No hardcoded AWS account IDs or ARNs  
✅ No API keys or access tokens  
✅ No private keys or certificates  

## .gitignore Configuration

The `.gitignore` file is configured to exclude:

- `target/` - Maven build artifacts
- `cdk.out/` - CDK CloudFormation templates
- `.idea/` - IDE configuration
- `.env*` - Environment files
- `node_modules/` - Node dependencies
- AWS credential files

## Running the Project Locally

When developing locally:

1. **Set AWS credentials** via `~/.aws/credentials` or environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. **Never** hardcode credentials in configuration files
3. **Use IAM roles** when deploying to AWS (Lambda, EC2, ECS, etc.)

### Example: Setting AWS Credentials (macOS/Linux)

```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=eu-central-1
```

Or use the AWS CLI:

```bash
aws configure
```

## Deployment Best Practices

When deploying to AWS:

1. **Use IAM roles** instead of access keys
2. **Rotate credentials regularly**
3. **Use temporary security credentials** (STS AssumeRole)
4. **Enable CloudTrail** for audit logging
5. **Use AWS Secrets Manager** for sensitive application secrets
6. **Use VPC endpoints** for private communication with AWS services
7. **Enable MFA** on AWS account root user
8. **Use resource-based policies** to restrict access

## Lambda Execution Roles

Each Lambda in this project has a specific IAM role with minimal required permissions:

- **TaskCreator**: DynamoDB write access
- **TaskGetter**: DynamoDB read access
- **TaskStatusPublisher**: EventBridge PutEvents to `TaskManagerBus`
- **TaskStatusUpdater**: DynamoDB write access
- **DemoProcessor**: EventBridge PutEvents to `TaskManagerBus`

## Secrets Management in AWS

For application secrets (if needed in the future):

1. **AWS Secrets Manager**: Store sensitive data (API keys, passwords, database credentials)
2. **AWS Systems Manager Parameter Store**: Store non-sensitive configuration
3. **KMS**: Encrypt sensitive data at rest

Example Lambda code to fetch secrets:

```java
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

SecretsManagerClient client = SecretsManagerClient.builder().build();
GetSecretValueResponse response = client.getSecretValue(GetSecretValueRequest.builder()
        .secretId("my-secret-name")
        .build());
String secret = response.secretString();
```

## Reporting Security Issues

If you discover a security vulnerability, **do not** open a public issue. Instead:

1. Contact the maintainer privately
2. Provide detailed information about the vulnerability
3. Allow time for a fix before public disclosure

## Further Reading

- [AWS Security Best Practices](https://aws.amazon.com/architecture/security-identity-compliance/)
- [OWASP Top 10](https://owasp.org/Top10/)
- [AWS Lambda Security](https://docs.aws.amazon.com/lambda/latest/dg/security.html)

