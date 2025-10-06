/**
 * 유저 승격 요청 폼 검증 유틸리티
 */
class RoleUpgradeValidator {

    /**
     * 회사명/공간명 검증
     */
    static validateCompany(company) {
        if (!company || company.trim() === '') {
            return { isValid: false, message: '회사명을 입력해주세요.' };
        }

        if (company.trim().length < 2) {
            return { isValid: false, message: '회사명은 2자 이상 입력해주세요.' };
        }

        if (company.trim().length > 100) {
            return { isValid: false, message: '회사명은 100자 이하로 입력해주세요.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 사업자등록번호 검증
     */
    static validateBusinessNumber(businessNumber) {
        if (!businessNumber || businessNumber.trim() === '') {
            return { isValid: false, message: '사업자등록번호를 입력해주세요.' };
        }

        const cleanNumber = businessNumber.replace(/[^0-9]/g, '');

        if (cleanNumber.length !== 10) {
            return { isValid: false, message: '사업자등록번호는 10자리 숫자여야 합니다.' };
        }

        if (!this.isValidBusinessNumber(cleanNumber)) {
            return { isValid: false, message: '올바르지 않은 사업자등록번호입니다.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 권한 검증
     */
    static validatePermission(permission) {
        const validPermissions = ['viewer', 'editor', 'admin'];

        if (!permission || permission.trim() === '') {
            return { isValid: false, message: '권한을 선택해주세요.' };
        }

        if (!validPermissions.includes(permission)) {
            return { isValid: false, message: '올바른 권한을 선택해주세요.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 추가 작성 사항 검증
     */
    static validateAdditional(additional) {
        if (!additional || additional.trim() === '') {
            return { isValid: true, message: '' };
        }

        if (additional.trim().length > 1000) {
            return { isValid: false, message: '추가 작성 사항은 1000자 이하로 입력해주세요.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 파일 검증 (선택사항으로 변경)
     */
    static validateFile(file) {
        // 파일이 없어도 유효 (선택사항)
        if (!file) {
            return { isValid: true, message: '' };
        }

        // 파일 크기 제한 (10MB)
        const maxSize = 10 * 1024 * 1024;
        if (file.size > maxSize) {
            return { isValid: false, message: '파일 크기는 10MB 이하여야 합니다.' };
        }

        // 파일 타입 검증
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
        if (!allowedTypes.includes(file.type)) {
            return { isValid: false, message: 'JPG, PNG, PDF 파일만 업로드 가능합니다.' };
        }

        return { isValid: true, message: '' };
    }

    /**
     * 사업자등록번호 체크섬 검증
     */
    static isValidBusinessNumber(businessNumber) {
        if (businessNumber.length !== 10) return false;

        const weights = [1, 3, 7, 1, 3, 7, 1, 3, 5];
        let sum = 0;

        for (let i = 0; i < 9; i++) {
            sum += parseInt(businessNumber[i]) * weights[i];
        }

        sum += Math.floor((parseInt(businessNumber[8]) * 5) / 10);
        const checkDigit = (10 - (sum % 10)) % 10;

        return checkDigit === parseInt(businessNumber[9]);
    }

    /**
     * 전체 폼 검증
     */
    static validateForm(formData, activeTab) {
        const errors = {};

        // 회사명/공간명 검증
        const company = formData.get('company');
        const companyValidation = this.validateCompany(company);
        if (!companyValidation.isValid) {
            errors.company = companyValidation.message;
        }

        // 사업자등록번호 검증
        const businessNumber = formData.get('businessNumber');
        const businessValidation = this.validateBusinessNumber(businessNumber);
        if (!businessValidation.isValid) {
            errors.businessNumber = businessValidation.message;
        }

        // 권한 검증 (공간 제공자만)
        if (activeTab === 'host') {
            const permission = formData.get('permission');
            const permissionValidation = this.validatePermission(permission);
            if (!permissionValidation.isValid) {
                errors.permission = permissionValidation.message;
            }
        }

        // 추가 작성 사항 검증
        const additional = formData.get('additional');
        const additionalValidation = this.validateAdditional(additional);
        if (!additionalValidation.isValid) {
            errors.additional = additionalValidation.message;
        }

        return {
            isValid: Object.keys(errors).length === 0,
            errors: errors
        };
    }

    /**
     * 실시간 필드 검증
     */
    static validateField(fieldName, value, activeTab) {
        switch (fieldName) {
            case 'company':
                return this.validateCompany(value);
            case 'businessNumber':
                return this.validateBusinessNumber(value);
            case 'permission':
                return this.validatePermission(value);
            case 'additional':
                return this.validateAdditional(value);
            default:
                return { isValid: true, message: '' };
        }
    }

    /**
     * 사업자등록번호 포맷팅
     */
    static formatBusinessNumber(value) {
        if (!value) return '';

        const numbers = value.replace(/[^0-9]/g, '');

        if (numbers.length <= 3) {
            return numbers;
        } else if (numbers.length <= 5) {
            return numbers.replace(/(\d{3})(\d+)/, '$1-$2');
        } else {
            return numbers.replace(/(\d{3})(\d{2})(\d+)/, '$1-$2-$3');
        }
    }

    /**
     * 파일명 포맷팅
     */
    static formatFileName(fileName, maxLength = 30) {
        if (!fileName) return '';

        if (fileName.length <= maxLength) return fileName;

        const extension = fileName.split('.').pop();
        const name = fileName.substring(0, fileName.lastIndexOf('.'));
        const truncatedName = name.substring(0, maxLength - extension.length - 4);

        return `${truncatedName}...${extension}`;
    }

    /**
     * 파일 크기 포맷팅
     */
    static formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}