# Product Image Upload Guide

## Directory Structure

```
d:/eclipse_workspace/react/4beans-moa/
└── uploads/
    └── product-image/
        ├── product1.png
        ├── product2.jpg
        └── ...
```

## Configuration

### application.properties
```properties
app.upload.product-image-dir=uploads/product-image/
app.upload.product-image-url-prefix=/uploads/product-image/
```

## Usage

### 1. Add Image to Directory

Place your product image in `d:/eclipse_workspace/react/4beans-moa/uploads/product-image/`

Example: `chatgpt-logo.png`

### 2. Update Database

Store the **relative URL path** in the database:

```sql
UPDATE product 
SET image = '/uploads/product-image/chatgpt-logo.png' 
WHERE product_id = 1;
```

### 3. Access Image

**Development:**
```
http://localhost:8080/uploads/product-image/chatgpt-logo.png
```

**Frontend automatically requests:**
```tsx
<img src={product.image} />
// Renders: <img src="/uploads/product-image/chatgpt-logo.png" />
```

## Production Deployment

### 1. Create directory on server:
```bash
mkdir -p /var/www/uploads/product-image
chmod 755 /var/www/uploads/product-image
```

### 2. Update application.properties:
```properties
app.upload.product-image-dir=/var/www/uploads/product-image/
```

### 3. Upload images to server:
```bash
scp uploads/product-image/* user@server:/var/www/uploads/product-image/
```

## Important Notes

✅ **DO**: Store relative paths in DB (`/uploads/product-image/image.png`)  
❌ **DON'T**: Store absolute URLs (`http://localhost:8080/uploads/product-image/image.png`)

✅ **DO**: Use forward slashes (`/uploads/product-image/`)  
❌ **DON'T**: Use backslashes (`\uploads\product-image\`)
